import java.io.*;
import java.util.*;

/*
 * Testador.java
 *
 * Orquestra a geração de grafos (via geradorgrafos) e a execução dos
 * algoritmos de Fleury (via CaminhoEuleriano do Main).
 *
 * Fluxo:
 *   1. Quais algoritmos testar  (Fleury-Naive / Fleury-Tarjan / Ambos)
 *   2. Quais tipos de grafo     (Euleriano / Semi / Nao / Todos)
 *   3. Quais tamanhos           (100 / 1.000 / 10.000 / 100.000 / Todos)
 *   4. Quantas rodadas por (arquivo x algoritmo)
 *   5. Verifica pastas; gera 1 grafo por combinação ausente
 *   6. Executa testes e grava tempos.txt
 */
public class Testador {

    static final String[] TIPOS     = {"euleriano", "semieuleriano", "naoeuleriano"};
    static final int[]    TIPOS_OP1 = {1, 2, 3};

    static final int[] TAMANHOS      = {100, 1_000, 10_000, 100_000};
    static final int[] TAMANHOS_OP2  = {1,   2,     3,      4};

    static final int LARGURA_BARRA = 36;

    private static final Scanner sc = new Scanner(System.in);

    // -----------------------------------------------------------------------
    public static void main(String[] args) {

        System.out.println("=== TESTADOR DE ALGORITMOS DE FLEURY ===\n");

        List<CaminhoEuleriano.Modo> modos = escolherAlgoritmos();
        if (modos.isEmpty()) return;

        List<Integer> tiposIdx = escolherTipos();
        if (tiposIdx.isEmpty()) return;

        List<Integer> tamanhosIdx = escolherTamanhos();
        if (tamanhosIdx.isEmpty()) return;

        System.out.print("\nNumero de rodadas por (arquivo x algoritmo): ");
        int rodadas = lerInt();
        if (rodadas < 1) { System.out.println("Numero invalido."); return; }

        System.out.println("\nVerificando grafos necessarios...");
        garantirGrafos(tiposIdx, tamanhosIdx);

        System.out.println("\nIniciando testes...\n");
        executarTestes(modos, tiposIdx, tamanhosIdx, rodadas);
    }

    // -----------------------------------------------------------------------
    // Menus
    // -----------------------------------------------------------------------
    private static List<CaminhoEuleriano.Modo> escolherAlgoritmos() {
        System.out.println("Algoritmos:");
        System.out.println("  1. Fleury-Naive");
        System.out.println("  2. Fleury-Tarjan");
        System.out.println("  3. Ambos");
        System.out.print("Opcao: ");
        int op = lerInt();
        List<CaminhoEuleriano.Modo> lista = new ArrayList<>();
        switch (op) {
            case 1: lista.add(CaminhoEuleriano.Modo.NAIVE);  break;
            case 2: lista.add(CaminhoEuleriano.Modo.TARJAN); break;
            case 3:
                lista.add(CaminhoEuleriano.Modo.NAIVE);
                lista.add(CaminhoEuleriano.Modo.TARJAN);
                break;
            default: System.out.println("Opcao invalida."); break;
        }
        return lista;
    }

    private static List<Integer> escolherTipos() {
        System.out.println("\nTipos de grafo:");
        System.out.println("  1. Euleriano");
        System.out.println("  2. Semi-euleriano");
        System.out.println("  3. Nao-euleriano");
        System.out.println("  4. Todos");
        System.out.print("Opcao: ");
        int op = lerInt();
        List<Integer> lista = new ArrayList<>();
        switch (op) {
            case 1: lista.add(0); break;
            case 2: lista.add(1); break;
            case 3: lista.add(2); break;
            case 4: lista.add(0); lista.add(1); lista.add(2); break;
            default: System.out.println("Opcao invalida."); break;
        }
        return lista;
    }

    private static List<Integer> escolherTamanhos() {
        System.out.println("\nTamanho dos grafos (numero de vertices):");
        System.out.println("  1.       100 vertices");
        System.out.println("  2.     1.000 vertices");
        System.out.println("  3.    10.000 vertices");
        System.out.println("  4.   100.000 vertices");
        System.out.println("  5. Todos");
        System.out.print("Opcao: ");
        int op = lerInt();
        List<Integer> lista = new ArrayList<>();
        switch (op) {
            case 1: lista.add(0); break;
            case 2: lista.add(1); break;
            case 3: lista.add(2); break;
            case 4: lista.add(3); break;
            case 5: lista.add(0); lista.add(1); lista.add(2); lista.add(3); break;
            default: System.out.println("Opcao invalida."); break;
        }
        return lista;
    }

    // -----------------------------------------------------------------------
    // Garante existência dos grafos necessários, gerando os ausentes
    // -----------------------------------------------------------------------
    private static void garantirGrafos(List<Integer> tiposIdx, List<Integer> tamanhosIdx) {
        for (int ti : tiposIdx) {
            String tipo = TIPOS[ti];
            int    op1  = TIPOS_OP1[ti];
            for (int vi : tamanhosIdx) {
                int    n    = TAMANHOS[vi];
                int    op2  = TAMANHOS_OP2[vi];
                String path = "grafos" + n + tipo;

                File[] arqs = pathfilefinder.encontrarArquivos(tipo, n);
                if (arqs != null && arqs.length > 0) {
                    System.out.println("  [ok]      " + path
                            + " (" + arqs.length + " grafo(s))");
                    continue;
                }

                System.out.println("  [gerando] " + path + "...");
                geradorgrafos.gerarLote(1, op1, op2);
                System.out.println();

                arqs = pathfilefinder.encontrarArquivos(tipo, n);
                if (arqs != null && arqs.length > 0) {
                    System.out.println("  [ok]      " + path + " gerado.");
                } else {
                    System.out.println("  [ERRO]    Falha ao gerar " + path);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Executa os testes e grava tempos.txt
    // -----------------------------------------------------------------------
    private static void executarTestes(List<CaminhoEuleriano.Modo> modos,
                                        List<Integer> tiposIdx,
                                        List<Integer> tamanhosIdx,
                                        int rodadas) {

        String[] nomeModos = modos.stream()
                .map(m -> m == CaminhoEuleriano.Modo.NAIVE ? "fleury-naive" : "fleury-tarjan")
                .toArray(String[]::new);

        List<EntradaTeste> entradas = coletarEntradas(tiposIdx, tamanhosIdx);
        int totalExec  = entradas.size() * modos.size() * rodadas;
        int execFeitas = 0;

        List<String> linhas = new ArrayList<>();
        linhas.add(String.format("%-16s  %-14s  %10s  %6s  %14s  %14s",
                "algoritmo", "tipo", "vertices", "rodada", "tempo_ms", "tempo_s"));
        linhas.add("------------------------------------------------------------------------------------");

        // acumula tempos para calcular media — chave: "alg|tipo|vertices"
        Map<String, List<Double>> acumulador = new LinkedHashMap<>();

        imprimirBarra(totalExec, 0, "iniciando");

        for (EntradaTeste entrada : entradas) {
            try {
                Grafos.lerGrafo(entrada.arquivo);
            } catch (FileNotFoundException e) {
                System.out.println("\n[ERRO] Arquivo nao encontrado: " + entrada.arquivo);
                execFeitas += modos.size() * rodadas;
                continue;
            }

            // Clone original para restaurar antes de cada algoritmo
            Vertice[] backupOriginal = Grafos.clonarGrafo();

            for (int ai = 0; ai < modos.size(); ai++) {
                CaminhoEuleriano.Modo modo   = modos.get(ai);
                String               nomeAlg = nomeModos[ai];
                String               chave   = nomeAlg + "|" + entrada.tipo + "|" + Grafos.vertice;
                acumulador.putIfAbsent(chave, new ArrayList<>());

                for (int r = 1; r <= rodadas; r++) {
                    imprimirBarra(totalExec, execFeitas,
                            nomeAlg + " | " + entrada.tipo
                            + " " + Grafos.vertice + "v (rod." + r + ")");

                    // Restaura estado original antes de cada rodada
                    Grafos.restaurarGrafo(backupOriginal);
                    Vertice[] backup = Grafos.clonarGrafo();

                    long ini   = System.nanoTime();
                    CaminhoEuleriano.executar(modo);
                    long durNs = System.nanoTime() - ini;

                    Grafos.restaurarGrafo(backup);
                    execFeitas++;

                    double durMs = durNs / 1_000_000.0;
                    acumulador.get(chave).add(durMs);

                    linhas.add(String.format("%-16s  %-14s  %10d  %6d  %14.3f  %14.6f",
                            nomeAlg, entrada.tipo, Grafos.vertice, r,
                            durMs, durNs / 1_000_000_000.0));
                }
            }
        }

        imprimirBarra(totalExec, totalExec, "concluido");
        System.out.println();
        System.out.println();

        // Medias
        linhas.add("");
        linhas.add("=== MEDIAS ===");
        linhas.add(String.format("%-16s  %-14s  %10s  %14s  %14s",
                "algoritmo", "tipo", "vertices", "media_ms", "media_s"));
        linhas.add("------------------------------------------------------------------------------------");
        for (Map.Entry<String, List<Double>> e : acumulador.entrySet()) {
            String[] p     = e.getKey().split("\\|");
            double   media = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            linhas.add(String.format("%-16s  %-14s  %10s  %14.3f  %14.6f",
                    p[0], p[1], p[2], media, media / 1000.0));
        }

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tempos.txt")))) {
            for (String l : linhas) pw.println(l);
            System.out.println("Resultados gravados em: "
                    + new File("tempos.txt").getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Erro ao gravar tempos.txt: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Coleta arquivos respeitando tipos E tamanhos selecionados
    // -----------------------------------------------------------------------
    private static List<EntradaTeste> coletarEntradas(List<Integer> tiposIdx,
                                                       List<Integer> tamanhosIdx) {
        List<EntradaTeste> lista = new ArrayList<>();
        for (int ti : tiposIdx) {
            String tipo = TIPOS[ti];
            for (int vi : tamanhosIdx) {
                int    n    = TAMANHOS[vi];
                File[] arqs = pathfilefinder.encontrarArquivos(tipo, n);
                if (arqs == null || arqs.length == 0) continue;
                Arrays.sort(arqs, Comparator.comparing(File::getAbsolutePath));
                for (File f : arqs) lista.add(new EntradaTeste(f, tipo, n));
            }
        }
        return lista;
    }

    // -----------------------------------------------------------------------
    // Barra de progresso
    // -----------------------------------------------------------------------
    private static void imprimirBarra(int total, int feitos, String desc) {
        int pre = total == 0 ? 0 : (feitos * LARGURA_BARRA) / total;
        int pct = total == 0 ? 0 : (feitos * 100) / total;
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < pre;            i++) b.append('\u2588');
        for (int i = pre; i < LARGURA_BARRA; i++) b.append('\u2591');
        b.append("]");
        String d = desc.length() > 52 ? desc.substring(0, 52) : desc;
        System.out.printf("\r%s %3d%%  %s", b, pct, d);
        System.out.flush();
    }

    // -----------------------------------------------------------------------
    private static int lerInt() {
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    static class EntradaTeste {
        File   arquivo;
        String tipo;
        int    vertices;
        EntradaTeste(File arquivo, String tipo, int vertices) {
            this.arquivo  = arquivo;
            this.tipo     = tipo;
            this.vertices = vertices;
        }
    }
}