import java.io.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Main - Problema dos k-Centros Interativo
 *
 * Agora verifica os arquivos no diretório, estima o custo computacional 
 * e questiona o usuário antes de engargalar a máquina.
 */
public class Main {

    // Constante de velocidade aproximada do processador 
    // Estimativa: ~100 milhões de iterações do laço interno por segundo
    private static final double OPERACOES_POR_SEGUNDO = 100_000_000.0;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Sistema de Resolução de k-Centros ---");
        System.out.println("Buscando arquivos .txt no diretório atual...\n");

        // 1. Mapeamento dos arquivos no computador
        File dir = new File(System.getProperty("user.dir"));
        File[] arquivos = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum arquivo .txt encontrado no diretório atual.");
            return;
        }

        for (int i = 0; i < arquivos.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, arquivos[i].getName());
        }

        System.out.print("\nEscolha o número do arquivo que deseja processar: ");
        int escolha = scanner.nextInt();
        if (escolha < 1 || escolha > arquivos.length) {
            System.out.println("Escolha inválida. Encerrando.");
            return;
        }

        File arquivoEscolhido = arquivos[escolha - 1];
        String nomeArquivo = arquivoEscolhido.getName();

        // 2. Identificação do Formato
        boolean isPmed = nomeArquivo.toLowerCase().contains("pmed");
        if (!isPmed) {
            System.out.print("O arquivo está no formato OR-Library (pmed)? (S/N): ");
            String resp = scanner.next();
            isPmed = resp.equalsIgnoreCase("s");
        }

        FloydWarshall fw;
        int k = 2; // Valor padrão

        if (isPmed) {
            LeitorPmed lp = new LeitorPmed(arquivoEscolhido.getAbsolutePath());
            fw = lp.getFloydWarshall();
            k = lp.getK();
            System.out.printf("\nInstância OR-Library carregada: %d vértices, k=%d%n%n", lp.getV(), k);
        } else {
            fw = new FloydWarshall(arquivoEscolhido.getAbsolutePath());
            System.out.print("Digite o valor de k (número de centros a alocar): ");
            k = scanner.nextInt();
            System.out.printf("\nGrafo próprio carregado: %d vértices, k=%d%n%n", fw.tamanho(), k);
        }

        int n = fw.tamanho();

        if (n <= 20) {
            System.out.println("Matriz de distâncias (Floyd-Warshall):");
            fw.imprimir();
            System.out.println();
        }

        System.out.println("-----------------------------------------");

        // APROXIMADO (sempre roda)
        System.out.println("Executando método APROXIMADO (Greedy)...");
        long t0 = System.currentTimeMillis();
        KCentros.Resultado aprox = KCentros.aproximado(fw, k);
        long tAprox = System.currentTimeMillis() - t0;
        System.out.println(aprox);
        System.out.printf("  Tempo: %d ms%n", tAprox);

        System.out.println("-----------------------------------------");
        System.out.println("Analisando viabilidade do método EXATO...");

        // 3. Estimativa de Tempo
        BigInteger operacoes = estimarOperacoes(n, k);
        double segundosEstimados = operacoes.doubleValue() / OPERACOES_POR_SEGUNDO;

        System.out.printf("  Operações estimadas: %s%n", formatarNumeroGrande(operacoes));
        System.out.printf("  Tempo estimado: %s%n", formatarTempo(segundosEstimados));

        if (segundosEstimados > 60) {
            System.out.println("\n[ALERTA] O tempo de execução estimado é crítico.");
        }

        // 4. Decisão do Usuário
        System.out.print("\nDeseja prosseguir com a execução da força bruta (EXATO)? (S/N): ");
        String continuar = scanner.next();

        if (continuar.equalsIgnoreCase("s")) {
            System.out.println("\nIniciando método EXATO (Pressione Ctrl+C para abortar a qualquer momento)...");
            t0 = System.currentTimeMillis();
            KCentros.Resultado exato = KCentros.exato(fw, k);
            long tExato = System.currentTimeMillis() - t0;
            System.out.println(exato);
            System.out.printf("  Tempo Real: %d ms%n", tExato);

            System.out.println("-----------------------------------------");
            long gap = aprox.raio - exato.raio;
            System.out.printf("Gap aproximado vs exato: %+d (%.1f%%)%n",
                    gap, exato.raio == 0 ? 0.0 : 100.0 * gap / exato.raio);
        } else {
            System.out.println("\nExecução do método exato cancelada. Workflow finalizado.");
        }
        System.out.println("-----------------------------------------");

        scanner.close();
    }

    // --- MÉTODOS DE CÁLCULO E FORMATAÇÃO ---

    /**
     * Estima o número de operações baseado na complexidade O(C(n,k) * n * k)
     */
    private static BigInteger estimarOperacoes(int n, int k) {
        BigInteger combinacoes = calcularCombinacao(n, k);
        return combinacoes.multiply(BigInteger.valueOf((long) n * k));
    }

    /**
     * Calcula C(n,k) usando BigInteger para evitar overflow em grafos médios/grandes.
     */
    private static BigInteger calcularCombinacao(int n, int k) {
        if (k > n - k) {
            k = n - k; // Otimização de simetria: C(n, k) == C(n, n-k)
        }
        BigInteger res = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            res = res.multiply(BigInteger.valueOf(n - i + 1))
                       .divide(BigInteger.valueOf(i));
        }
        return res;
    }

    /**
     * Formata o tempo estimado para a unidade de tempo mais fácil de ler.
     */
    private static String formatarTempo(double segundos) {
        if (segundos == Double.POSITIVE_INFINITY) return "Infinito";
        if (segundos < 1) return String.format("%.4f segundos", segundos);
        if (segundos < 60) return String.format("%.1f segundos", segundos);
        if (segundos < 3600) return String.format("%.1f minutos", segundos / 60);
        if (segundos < 86400) return String.format("%.1f horas", segundos / 3600);
        if (segundos < 31536000) return String.format("%.1f dias", segundos / 86400);
        
        // Se for um valor gigantesco (ex: C(100, 20))
        double anos = segundos / 31536000;
        if (anos > 1_000_000) return String.format("%.2e anos (Milênios)", anos);
        return String.format("%.1f anos", anos);
    }

    /**
     * Formata números gigantes usando notação científica para manter o terminal limpo.
     */
    private static String formatarNumeroGrande(BigInteger num) {
        String str = num.toString();
        if (str.length() > 6) {
            return String.format("%s.%.2s x 10^%d", str.charAt(0), str.substring(1), str.length() - 1);
        }
        return str;
    }
}