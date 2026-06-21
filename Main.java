import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

public class Main {

    private static final double LIMITE_HORAS_PADRAO = 3.0;
    private static final DateTimeFormatter FMT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // ESTRUTURA: dados de um grafo carregado + resultados de execução
    // =========================================================================

    static class EntradaGrafo {
        final int            indice;       // posição na lista (1-based)
        final File           arquivo;
        final String         nome;
        FloydWarshall        fw;
        int                  k;
        boolean              carregado;
        String               erroCarregamento;

        // Resultados aproximado
        KCentros.Resultado   resultadoAprox;
        long                 tempoAproxMs;

        // Estimativa exato
        EstimadorTempo.Estimativa estimativa;

        // Resultados exato
        KCentros.Resultado   resultadoExato;
        long                 tempoExatoMs;

        EntradaGrafo(int indice, File arquivo) {
            this.indice  = indice;
            this.arquivo = arquivo;
            this.nome    = arquivo.getName();
        }
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        printSeparador('=', 62);
        System.out.println("    Sistema Ultra-Otimizado de K-Centros");
        printSeparador('=', 62);
        System.out.println("Buscando ficheiros .txt recursivamente no diretório atual...\n");

        // ── Busca arquivos ──────────────────────────────────────────────────
        List<File> listaArquivos = new ArrayList<>();
        Path dirInicial = Paths.get(System.getProperty("user.dir"));
        try (Stream<Path> caminhos = Files.walk(dirInicial)) {
            caminhos.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .sorted()
                    .forEach(p -> listaArquivos.add(p.toFile()));
        }

        if (listaArquivos.isEmpty()) {
            System.out.println("Nenhum ficheiro .txt encontrado.");
            return;
        }

        File[] arquivos = listaArquivos.toArray(new File[0]);
        for (int i = 0; i < arquivos.length; i++)
            System.out.printf("[%2d] %s%n", i + 1, arquivos[i].getAbsolutePath());

        System.out.println();
        printSeparador('-', 62);
        System.out.printf("[%2d] Executar TODOS os grafos automaticamente%n", arquivos.length + 1);
        printSeparador('-', 62);
        System.out.print("\nEscolha uma opção: ");
        int escolha = lerInt(scanner);

        if (escolha == arquivos.length + 1) {
            executarLote(arquivos, scanner);
        } else if (escolha >= 1 && escolha <= arquivos.length) {
            executarUnico(arquivos[escolha - 1], scanner);
        } else {
            System.out.println("Escolha inválida. Encerrando.");
        }

        scanner.close();
    }

    // =========================================================================
    // MODO LOTE: fluxo em 4 fases
    // =========================================================================

    private static void executarLote(File[] arquivos, Scanner scanner) throws Exception {

        // Nome do CSV com timestamp
        String csvNome = "resultados_kcentros_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            + ".csv";
        ExportadorCSV csv = new ExportadorCSV(csvNome);
        System.out.printf("\nResultados serão salvos em: %s%n", csvNome);

        // Monta lista de entradas
        List<EntradaGrafo> entradas = new ArrayList<>();
        for (int i = 0; i < arquivos.length; i++)
            entradas.add(new EntradaGrafo(i + 1, arquivos[i]));

        // ── FASE 1: carrega grafos ──────────────────────────────────────────
        System.out.println();
        printSeparador('=', 62);
        System.out.println("  FASE 1 — Carregando grafos");
        printSeparador('=', 62);
        for (EntradaGrafo e : entradas) {
            try {
                carregarGrafo(e);
                System.out.printf("  [%2d/%d] %-30s OK  (%d vértices, k=%d)%n",
                    e.indice, entradas.size(), e.nome, e.fw.tamanho(), e.k);
            } catch (Exception ex) {
                e.carregado       = false;
                e.erroCarregamento = ex.getMessage();
                System.out.printf("  [%2d/%d] %-30s ERRO: %s%n",
                    e.indice, entradas.size(), e.nome, ex.getMessage());
            }
        }

        List<EntradaGrafo> validos = entradas.stream()
            .filter(e -> e.carregado)
            .collect(Collectors.toList());

        if (validos.isEmpty()) {
            System.out.println("\nNenhum grafo válido encontrado. Encerrando.");
            return;
        }

        // ── FASE 2: todos os aproximados ────────────────────────────────────
        System.out.println();
        printSeparador('=', 62);
        System.out.printf("  FASE 2 — Métodos Aproximados (%d grafos)%n", validos.size());
        printSeparador('=', 62);

        for (EntradaGrafo e : validos) {
            System.out.printf("%n  [%2d/%d] %s%n", e.indice, validos.size(), e.nome);
            long t0 = System.currentTimeMillis();
            e.resultadoAprox = KCentros.aproximado(e.fw, e.k);
            e.tempoAproxMs   = System.currentTimeMillis() - t0;

            System.out.printf("         %-20s Raio=%-10s  Tempo=%s%n",
                e.resultadoAprox.metodo,
                raioStr(e.resultadoAprox.raio),
                formatMs(e.tempoAproxMs));
            System.out.printf("         Centros: %s%n", centrosStr(e.resultadoAprox.centros));

            csv.registrar(e.nome, e.fw.tamanho(), e.k,
                e.resultadoAprox, e.tempoAproxMs, null, null, null);
        }

        // ── FASE 3: análise de viabilidade dos exatos ───────────────────────
        System.out.println();
        printSeparador('=', 62);
        System.out.println("  FASE 3 — Calibrando estimativas para método Exato");
        printSeparador('=', 62);
        System.out.println("  (Executando micro-benchmark de CPU...)\n");

        for (EntradaGrafo e : validos) {
            e.estimativa = EstimadorTempo.estimar(e.fw, e.k, e.resultadoAprox);
        }

        // Ordena por tempo estimado (menor primeiro) para exibição
        List<EntradaGrafo> viaveis = validos.stream()
            .sorted(Comparator.comparingDouble(e -> e.estimativa.segundosEstimados))
            .collect(Collectors.toList());

        System.out.println("  Métodos exatos disponíveis para execução:\n");
        System.out.printf("  %-4s %-32s %-18s %-18s %-8s%n",
            "Nº", "Grafo", "Estimativa (c/poda)", "Pior caso", "Comb.");
        printSeparador('-', 90);

        double totalSegundos = 0;
        int ordemExato = 1;
        List<Integer> ordemIndices = new ArrayList<>(); // índice dentro de `viaveis`
        for (int vi = 0; vi < viaveis.size(); vi++) {
            EntradaGrafo e = viaveis.get(vi);
            totalSegundos += e.estimativa.segundosEstimados;
            ordemIndices.add(vi);
            System.out.printf("  [%2d] %-32s %-18s %-18s %-8s%n",
                ordemExato++,
                e.nome,
                formatSegundos(e.estimativa.segundosEstimados),
                formatSegundos(e.estimativa.segundosPiorCaso),
                EstimadorTempo.formatarNumeroGrande(e.estimativa.totalCombinacoes));
        }

        printSeparador('-', 90);
        System.out.printf("  Tempo total estimado (sequencial): %s%n%n",
            formatSegundos(totalSegundos));

        // ── FASE 4: seleção e execução dos exatos ───────────────────────────
        System.out.println("  Opções de execução dos métodos exatos:");
        System.out.println("  [1] Executar todos");
        System.out.printf ("  [2] Executar apenas com estimativa inferior a %.0f horas%n",
            LIMITE_HORAS_PADRAO);
        System.out.println("  [3] Selecionar manualmente");
        System.out.println("  [4] Cancelar execução dos métodos exatos");
        System.out.print("\n  Escolha: ");
        int opcaoExato = lerInt(scanner);

        List<EntradaGrafo> selecionados = new ArrayList<>();

        switch (opcaoExato) {
            case 1:
                selecionados.addAll(viaveis);
                break;
            case 2:
                selecionados = viaveis.stream()
                    .filter(e -> e.estimativa.segundosEstimados < LIMITE_HORAS_PADRAO * 3600)
                    .collect(Collectors.toList());
                System.out.printf("  → %d grafo(s) selecionados (estimativa < %.0f h).%n",
                    selecionados.size(), LIMITE_HORAS_PADRAO);
                break;
            case 3:
                System.out.print("  Digite os índices separados por vírgula (ex: 1,3,4): ");
                scanner.nextLine(); // limpa buffer
                String linha = scanner.nextLine().trim();
                for (String tok : linha.split(",")) {
                    try {
                        int idx = Integer.parseInt(tok.trim()) - 1;
                        if (idx >= 0 && idx < viaveis.size())
                            selecionados.add(viaveis.get(idx));
                        else
                            System.out.printf("  Índice %d ignorado (fora do intervalo).%n", idx + 1);
                    } catch (NumberFormatException ex) {
                        System.out.printf("  Valor '%s' ignorado.%n", tok.trim());
                    }
                }
                System.out.printf("  → %d grafo(s) selecionado(s).%n", selecionados.size());
                break;
            case 4:
            default:
                System.out.println("\n  Execução dos métodos exatos cancelada.");
                imprimirResumoFinal(validos, csvNome);
                return;
        }

        if (selecionados.isEmpty()) {
            System.out.println("\n  Nenhum grafo selecionado para o método exato.");
            imprimirResumoFinal(validos, csvNome);
            return;
        }

        // Exibe o plano antes de começar
        System.out.println();
        printSeparador('=', 62);
        System.out.printf("  FASE 4 — Métodos Exatos (%d grafos selecionados)%n", selecionados.size());
        printSeparador('=', 62);

        double totalSelecionado = selecionados.stream()
            .mapToDouble(e -> e.estimativa.segundosEstimados).sum();
        System.out.printf("  Tempo total estimado para seleção: %s%n%n",
            formatSegundos(totalSelecionado));

        for (int si = 0; si < selecionados.size(); si++) {
            EntradaGrafo e = selecionados.get(si);
            System.out.println();
            printSeparador('-', 62);
            System.out.printf("  Executando %d/%d: %s%n",
                si + 1, selecionados.size(), e.nome);
            System.out.printf("  Estimativa: %s | Cores: %d | Vel. calibrada: %.1fM comb/s%n",
                formatSegundos(e.estimativa.segundosEstimados),
                e.estimativa.cores,
                e.estimativa.opsCalibradas / 1_000_000);
            printSeparador('-', 62);

            long t0 = System.currentTimeMillis();

            // Thread de progresso com tempo decorrido e estimativa restante
            Thread monitor = iniciarMonitorComTempo(
                e.estimativa.totalCombinacoes,
                e.estimativa.segundosEstimados,
                t0);

            e.resultadoExato = KCentros.exato(e.fw, e.k, e.resultadoAprox);
            e.tempoExatoMs   = System.currentTimeMillis() - t0;

            monitor.interrupt();
            // Limpa a linha do monitor
            System.out.print("\r" + " ".repeat(80) + "\r");

            System.out.printf("  Resultado: %s%n", e.resultadoExato);
            System.out.printf("  Tempo real: %s%n", formatMs(e.tempoExatoMs));

            double erroEstimativa = e.estimativa.segundosEstimados - (e.tempoExatoMs / 1000.0);
            System.out.printf("  Diferença estimativa vs real: %s%s%n",
                erroEstimativa >= 0 ? "+" : "",
                formatSegundos(Math.abs(erroEstimativa)));

            long gap = e.resultadoAprox.raio - e.resultadoExato.raio;
            double gapPct = e.resultadoExato.raio == 0 ? 0.0
                : 100.0 * gap / e.resultadoExato.raio;
            System.out.printf("  Gap aprox vs exato: %+d (%.1f%%)%n", gap, gapPct);

            csv.registrar(e.nome, e.fw.tamanho(), e.k,
                e.resultadoExato, e.tempoExatoMs, e.estimativa, gap, gapPct);
        }

        imprimirResumoFinal(validos, csvNome);
    }

    // =========================================================================
    // MODO ÚNICO (arquivo individual — interativo)
    // =========================================================================

    private static void executarUnico(File arquivo, Scanner scanner) throws Exception {
        EntradaGrafo e = new EntradaGrafo(1, arquivo);
        carregarGrafo(e);

        int n = e.fw.tamanho();
        System.out.printf("\nGrafo: %s | %d vértices | k=%d%n", e.nome, n, e.k);

        if (n <= 20) {
            System.out.println("\nMatriz de distâncias (Floyd-Warshall):");
            e.fw.imprimir();
        }

        // Aproximado
        System.out.println("\n--- Método Aproximado ---");
        long t0 = System.currentTimeMillis();
        e.resultadoAprox = KCentros.aproximado(e.fw, e.k);
        e.tempoAproxMs   = System.currentTimeMillis() - t0;
        System.out.printf("%s%n", e.resultadoAprox);
        System.out.printf("Tempo: %s%n", formatMs(e.tempoAproxMs));

        // Estimativa
        System.out.println("\n--- Estimativa para Método Exato ---");
        System.out.println("Calibrando CPU...");
        e.estimativa = EstimadorTempo.estimar(e.fw, e.k, e.resultadoAprox);
        System.out.println(e.estimativa);

        System.out.printf("\nDeseja executar o método EXATO (estimativa: %s)? (S/N): ",
            formatSegundos(e.estimativa.segundosEstimados));
        String resp = scanner.next();
        if (!resp.equalsIgnoreCase("s")) {
            System.out.println("Execução cancelada.");
            return;
        }

        System.out.println("\n--- Método Exato ---");
        t0 = System.currentTimeMillis();
        Thread monitor = iniciarMonitorComTempo(
            e.estimativa.totalCombinacoes, e.estimativa.segundosEstimados, t0);

        e.resultadoExato = KCentros.exato(e.fw, e.k, e.resultadoAprox);
        e.tempoExatoMs   = System.currentTimeMillis() - t0;

        monitor.interrupt();
        System.out.print("\r" + " ".repeat(80) + "\r");

        System.out.printf("%s%n", e.resultadoExato);
        System.out.printf("Tempo real: %s%n", formatMs(e.tempoExatoMs));

        long gap = e.resultadoAprox.raio - e.resultadoExato.raio;
        double gapPct = e.resultadoExato.raio == 0 ? 0.0
            : 100.0 * gap / e.resultadoExato.raio;
        System.out.printf("Gap aprox vs exato: %+d (%.1f%%)%n", gap, gapPct);
    }

    // =========================================================================
    // CARREGAMENTO DE GRAFO
    // =========================================================================

    private static void carregarGrafo(EntradaGrafo e) throws Exception {
        String nome = e.nome.toLowerCase();
        boolean isPmed = nome.contains("pmed");

        if (isPmed) {
            LeitorPmed lp = new LeitorPmed(e.arquivo.getAbsolutePath());
            e.fw = lp.getFloydWarshall();
            e.k  = lp.getK();
        } else {
            e.fw = new FloydWarshall(e.arquivo.getAbsolutePath());
            e.k  = lerKDoArquivo(e.arquivo);
        }
        e.carregado = true;
    }

    private static int lerKDoArquivo(File arquivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            st.nextToken(); // V
            return Integer.parseInt(st.nextToken()); // K
        } catch (Exception e) {
            return 2;
        }
    }

    // =========================================================================
    // MONITOR DE PROGRESSO COM TEMPO DECORRIDO E ESTIMATIVA RESTANTE
    // =========================================================================

    private static Thread iniciarMonitorComTempo(
            BigInteger totalCombinacoes,
            double estimativaInicialSeg,
            long inicioMs) {

        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long decMs = System.currentTimeMillis() - inicioMs;
                double decSeg = decMs / 1000.0;
                double restSeg = Math.max(0, estimativaInicialSeg - decSeg);

                System.out.printf("\r  Decorrido: %-12s | Estimativa restante: %-14s",
                    formatSegundos(decSeg),
                    formatSegundos(restSeg));

                try { Thread.sleep(500); }
                catch (InterruptedException ex) { break; }
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    // =========================================================================
    // RESUMO FINAL
    // =========================================================================

    private static void imprimirResumoFinal(List<EntradaGrafo> validos, String csvNome) {
        System.out.println();
        printSeparador('=', 62);
        System.out.println("  RESUMO FINAL");
        printSeparador('=', 62);
        System.out.printf("  %-30s %-14s %-14s %-10s%n",
            "Grafo", "Raio Aprox", "Raio Exato", "Gap");
        printSeparador('-', 62);
        for (EntradaGrafo e : validos) {
            String raioExato = (e.resultadoExato != null)
                ? raioStr(e.resultadoExato.raio) : "—";
            String gap = (e.resultadoExato != null && e.resultadoAprox != null)
                ? String.format("%+d", e.resultadoAprox.raio - e.resultadoExato.raio)
                : "—";
            System.out.printf("  %-30s %-14s %-14s %-10s%n",
                e.nome,
                (e.resultadoAprox != null ? raioStr(e.resultadoAprox.raio) : "ERRO"),
                raioExato,
                gap);
        }
        printSeparador('-', 62);
        System.out.printf("  Resultados salvos em: %s%n", csvNome);
        printSeparador('=', 62);
    }

    // =========================================================================
    // UTILITÁRIOS DE FORMATAÇÃO
    // =========================================================================

    private static String formatMs(long ms) {
        if (ms < 1000) return ms + " ms";
        if (ms < 60000) return String.format("%.2f s", ms / 1000.0);
        if (ms < 3600000) return String.format("%.1f min", ms / 60000.0);
        return String.format("%.1f h", ms / 3600000.0);
    }

    /**
     * Formata segundos no estilo "1 h 42 min", "35 min", "12 s", "< 1 ms", etc.
     */
    static String formatSegundos(double seg) {
        if (Double.isInfinite(seg) || Double.isNaN(seg)) return "Infinito";
        if (seg < 0.001)  return "< 1 ms";
        if (seg < 1)      return String.format("%.0f ms", seg * 1000);
        if (seg < 60)     return String.format("%.1f s", seg);
        if (seg < 3600) {
            int min = (int)(seg / 60);
            int s   = (int)(seg % 60);
            return s > 0 ? String.format("%d min %d s", min, s)
                         : String.format("%d min", min);
        }
        int h   = (int)(seg / 3600);
        int min = (int)((seg % 3600) / 60);
        return min > 0 ? String.format("%d h %d min", h, min)
                       : String.format("%d h", h);
    }

    private static String raioStr(long raio) {
        return raio >= FloydWarshall.INF ? "INF" : String.valueOf(raio);
    }

    private static String centrosStr(int[] centros) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < centros.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(centros[i] + 1);
        }
        return sb.append("}").toString();
    }

    private static void printSeparador(char c, int n) {
        System.out.println(String.valueOf(c).repeat(n));
    }

    private static int lerInt(Scanner sc) {
        try { return sc.nextInt(); }
        catch (Exception e) { sc.nextLine(); return -1; }
    }
}
