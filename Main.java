import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    // Limiar para execução automática do método exato: 3 horas
    private static final double LIMITE_HORAS_EXATO = 3.0;
    private static final double LIMITE_SEGUNDOS_EXATO = LIMITE_HORAS_EXATO * 3600;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("==========================================================");
        System.out.println("    Sistema Ultra-Otimizado de K-Centros");
        System.out.println("==========================================================");
        System.out.println("Buscando ficheiros .txt recursivamente no diretório atual...\n");

        List<File> arquivosList = new ArrayList<>();
        Path diretorioInicial = Paths.get(System.getProperty("user.dir"));

        try (Stream<Path> caminhos = Files.walk(diretorioInicial)) {
            caminhos.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .sorted()
                    .forEach(p -> arquivosList.add(p.toFile()));
        } catch (IOException e) {
            System.out.println("Erro ao buscar arquivos: " + e.getMessage());
            return;
        }

        File[] arquivos = arquivosList.toArray(new File[0]);

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum ficheiro .txt encontrado.");
            return;
        }

        for (int i = 0; i < arquivos.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, arquivos[i].getAbsolutePath());
        }

        System.out.println("\n----------------------------------------------------------");
        System.out.printf("[%d] Executar TODOS os grafos automaticamente%n", arquivos.length + 1);
        System.out.println("----------------------------------------------------------");
        System.out.print("\nEscolha uma opção: ");
        int escolha = scanner.nextInt();

        if (escolha == arquivos.length + 1) {
            // ── MODO: EXECUTAR TODOS ──────────────────────────────────────────
            executarTodos(arquivos, scanner);
        } else if (escolha >= 1 && escolha <= arquivos.length) {
            // ── MODO: ARQUIVO ÚNICO ──────────────────────────────────────────
            executarUnico(arquivos[escolha - 1], scanner, null);
        } else {
            System.out.println("Escolha inválida. Encerrando.");
        }

        scanner.close();
    }

    // =========================================================================
    // MODO: EXECUTAR TODOS OS GRAFOS
    // =========================================================================

    private static void executarTodos(File[] arquivos, Scanner scanner) throws Exception {
        // Nome do CSV com timestamp para evitar sobrescrever execuções anteriores
        String csvNome = "resultados_kcentros_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            + ".csv";
        ExportadorCSV csv = new ExportadorCSV(csvNome);

        System.out.println("\n==========================================================");
        System.out.printf("  Execução em lote: %d grafos encontrados%n", arquivos.length);
        System.out.printf("  Resultados serão salvos em: %s%n", csvNome);
        System.out.println("  Método exato será executado se estimativa < 3 horas.");
        System.out.println("==========================================================\n");

        int executados = 0;
        int erros      = 0;

        for (int idx = 0; idx < arquivos.length; idx++) {
            File arquivo = arquivos[idx];
            System.out.printf("%n[%d/%d] Processando: %s%n",
                idx + 1, arquivos.length, arquivo.getName());
            System.out.println("----------------------------------------------------------");

            try {
                executarUnico(arquivo, null, csv);
                executados++;
            } catch (Exception e) {
                System.err.printf("  ERRO ao processar '%s': %s%n", arquivo.getName(), e.getMessage());
                erros++;
            }
        }

        System.out.printf("%n==========================================================");
        System.out.printf("%n  Lote concluído: %d executados, %d erros.", executados, erros);
        System.out.printf("%n  Resultados salvos em: %s%n", csv.getCaminhoArquivo());
        System.out.println("==========================================================");
    }

    // =========================================================================
    // EXECUÇÃO DE UM ÚNICO GRAFO
    // scanner == null → modo automático (não pergunta nada)
    // csv    == null → modo interativo (não exporta)
    // =========================================================================

    private static void executarUnico(File arquivo, Scanner scanner, ExportadorCSV csv) throws Exception {
        String nomeArquivo = arquivo.getName();
        boolean modoAutomatico = (scanner == null);

        // Detecta formato
        boolean isPmed = nomeArquivo.toLowerCase().contains("pmed");
        if (!isPmed && !modoAutomatico) {
            System.out.print("O ficheiro está no formato OR-Library (pmed)? (S/N): ");
            String resp = scanner.next();
            isPmed = resp.equalsIgnoreCase("s");
        }

        // Carrega o grafo
        FloydWarshall fw;
        int k;

        if (isPmed) {
            LeitorPmed lp = new LeitorPmed(arquivo.getAbsolutePath());
            fw = lp.getFloydWarshall();
            k  = lp.getK();
            System.out.printf("  Instância OR-Library: %d vértices, k=%d%n", lp.getV(), k);
        } else {
            fw = new FloydWarshall(arquivo.getAbsolutePath());
            if (modoAutomatico) {
                // Tenta ler k do arquivo (1ª linha: V K)
                k = lerKDoArquivo(arquivo);
                System.out.printf("  Grafo próprio: %d vértices, k=%d%n", fw.tamanho(), k);
            } else {
                System.out.print("  Digite o valor de k (número de centros): ");
                k = scanner.nextInt();
                System.out.printf("  Grafo carregado: %d vértices, k=%d%n", fw.tamanho(), k);
            }
        }

        int n = fw.tamanho();

        if (n <= 20 && !modoAutomatico) {
            System.out.println("\n  Matriz de distâncias (Floyd-Warshall):");
            fw.imprimir();
        }

        // ── MÉTODO APROXIMADO ────────────────────────────────────────────────
        System.out.println("\n  [APROXIMADO] Executando...");
        long t0 = System.currentTimeMillis();
        KCentros.Resultado aprox = KCentros.aproximado(fw, k);
        long tAprox = System.currentTimeMillis() - t0;
        System.out.printf("  %s%n", aprox);
        System.out.printf("  Tempo: %d ms%n", tAprox);

        // Calcula estimativa com o novo estimador
        System.out.println("\n  [ESTIMATIVA] Calibrando velocidade real da CPU...");
        EstimadorTempo.Estimativa estimativa = EstimadorTempo.estimar(fw, k, aprox);
        System.out.printf("  %s%n", estimativa);

        // Registra aproximado no CSV
        if (csv != null) {
            csv.registrar(nomeArquivo, n, k, aprox, tAprox, estimativa, null, null);
        }

        // ── MÉTODO EXATO ─────────────────────────────────────────────────────
        boolean executarExato;
        if (modoAutomatico) {
            executarExato = estimativa.segundosEstimados < LIMITE_SEGUNDOS_EXATO;
            if (executarExato) {
                System.out.printf("%n  [EXATO] Estimativa %s < 3h → executando automaticamente...%n",
                    EstimadorTempo.formatarTempo(estimativa.segundosEstimados));
            } else {
                System.out.printf("%n  [EXATO] Estimativa %s ≥ 3h → pulando método exato.%n",
                    EstimadorTempo.formatarTempo(estimativa.segundosEstimados));
            }
        } else {
            System.out.printf("%n  Deseja executar o método EXATO (estimativa: %s)? (S/N): ",
                EstimadorTempo.formatarTempo(estimativa.segundosEstimados));
            String resp = scanner.next();
            executarExato = resp.equalsIgnoreCase("s");
        }

        if (executarExato) {
            System.out.println("  [EXATO] Iniciando barramento paralelo multi-thread...");
            t0 = System.currentTimeMillis();
            KCentros.Resultado exato = KCentros.exato(fw, k, aprox);
            long tExato = System.currentTimeMillis() - t0;
            System.out.printf("  %s%n", exato);
            System.out.printf("  Tempo real: %d ms%n", tExato);

            // Estimativa refinada (retrospectiva)
            double taxaObservada = KCentros.calcularCombinacao(n, k).doubleValue() / (tExato / 1000.0);
            System.out.printf("  Velocidade real observada: %.1fM comb/s%n",
                taxaObservada / 1_000_000);

            long gap = aprox.raio - exato.raio;
            double gapPct = exato.raio == 0 ? 0.0 : 100.0 * gap / exato.raio;
            System.out.printf("  Gap aprox vs exato: %+d (%.1f%%)%n", gap, gapPct);

            if (csv != null) {
                csv.registrar(nomeArquivo, n, k, exato, tExato, estimativa, gap, gapPct);
            }
        }

        System.out.println("----------------------------------------------------------");
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    /**
     * Tenta ler o valor de k da primeira linha do arquivo (formato "V K").
     * Se não encontrar, retorna 2 como padrão.
     */
    private static int lerKDoArquivo(File arquivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            int v = Integer.parseInt(st.nextToken()); // V (ignorado aqui)
            int k = Integer.parseInt(st.nextToken()); // K
            return k;
        } catch (Exception e) {
            return 2; // valor padrão
        }
    }
}
