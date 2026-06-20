import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    private static final double OPERACOES_POR_SEGUNDO_NUCLEO = 120_000_000.0;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Sistema Ultra-Otimizado de K-Centros ---");
        System.out.println("Buscando ficheiros .txt no diretório atual e em TODAS as subpastas...\n");

        List<File> arquivosList = new ArrayList<>();
        Path diretorioInicial = Paths.get(System.getProperty("user.dir"));

        // Usando Files.walk para buscar recursivamente em todos os subdiretórios (Cross-platform: Windows/Linux)
        try (Stream<Path> caminhos = Files.walk(diretorioInicial)) {
            caminhos.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt"))
                    .forEach(p -> arquivosList.add(p.toFile()));
        } catch (IOException e) {
            System.out.println("Erro ao buscar arquivos nas pastas: " + e.getMessage());
            return;
        }

        File[] arquivos = arquivosList.toArray(new File[0]);

        if (arquivos == null || arquivos.length == 0) {
            System.out.println("Nenhum ficheiro .txt encontrado no diretório atual ou subpastas.");
            return;
        }

        for (int i = 0; i < arquivos.length; i++) {
            // Mostrando o caminho absoluto para diferenciar arquivos com o mesmo nome em pastas distintas
            System.out.printf("[%d] %s%n", i + 1, arquivos[i].getAbsolutePath());
        }

        System.out.print("\nEscolha o número do ficheiro que deseja processar: ");
        int escolha = scanner.nextInt();
        if (escolha < 1 || escolha > arquivos.length) {
            System.out.println("Escolha inválida. Encerrando.");
            return;
        }

        File arquivoEscolhido = arquivos[escolha - 1];
        String nomeArquivo = arquivoEscolhido.getName();

        boolean isPmed = nomeArquivo.toLowerCase().contains("pmed");
        if (!isPmed) {
            System.out.print("O ficheiro está no formato OR-Library (pmed)? (S/N): ");
            String resp = scanner.next();
            isPmed = resp.equalsIgnoreCase("s");
        }

        FloydWarshall fw;
        int k = 2;

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
            System.out.println("Matriz de distâncias linearizada (Floyd-Warshall View):");
            fw.imprimir();
            System.out.println();
        }

        System.out.println("-----------------------------------------");
        System.out.println("Executando método APROXIMADO (Greedy Farthest-Point)...");
        long t0 = System.currentTimeMillis();
        KCentros.Resultado aprox = KCentros.aproximado(fw, k);
        long tAprox = System.currentTimeMillis() - t0;
        System.out.println(aprox);
        System.out.printf("  Tempo: %d ms%n", tAprox);

        System.out.println("-----------------------------------------");
        System.out.println("Analisando viabilidade do método EXATO PARALELO...");

        BigInteger operacoes = KCentros.calcularCombinacao(n, k).multiply(BigInteger.valueOf((long) n * k));
        int cores = Runtime.getRuntime().availableProcessors();
        double segundosPiorCenario = operacoes.doubleValue() / (OPERACOES_POR_SEGUNDO_NUCLEO * cores);

        System.out.printf("  Núcleos lógicos detectados no sistema: %d%n", cores);
        System.out.printf("  Operações combinatórias estimadas: %s%n", formatarNumeroGrande(operacoes));
        System.out.printf("  Tempo máximo teórico (Sem podas): %s%n", formatarTempo(segundosPiorCenario));

        System.out.print("\nDeseja disparar a execução concorrente por força bruta (EXATO)? (S/N): ");
        String continuar = scanner.next();

        if (continuar.equalsIgnoreCase("s")) {
            System.out.println("\nIniciando barramento paralelo multi-thread...");
            t0 = System.currentTimeMillis();
            KCentros.Resultado exato = KCentros.exato(fw, k, aprox);
            long tExato = System.currentTimeMillis() - t0;
            System.out.println(exato);
            System.out.printf("  Tempo Real Concorrente: %d ms%n", tExato);

            System.out.println("-----------------------------------------");
            long gap = aprox.raio - exato.raio;
            System.out.printf("Gap aproximado vs exato real: %+d (%.1f%%)%n",
                    gap, exato.raio == 0 ? 0.0 : 100.0 * gap / exato.raio);
        } else {
            System.out.println("\nExecução abortada pelo usuário. Workflow finalizado.");
        }
        System.out.println("-----------------------------------------");

        scanner.close();
    }

    private static String formatarTempo(double segundos) {
        if (segundos == Double.POSITIVE_INFINITY) return "Infinito";
        if (segundos < 1) return String.format("%.4f segundos", segundos);
        if (segundos < 60) return String.format("%.1f segundos", segundos);
        if (segundos < 3600) return String.format("%.1f minutos", segundos / 60);
        if (segundos < 86400) return String.format("%.1f horas", segundos / 3600);
        return String.format("%.1f dias", segundos / 86400);
    }

    private static String formatarNumeroGrande(BigInteger num) {
        String str = num.toString();
        if (str.length() > 6) {
            return String.format("%s.%.2s x 10^%d", str.charAt(0), str.substring(1), str.length() - 1);
        }
        return str;
    }
}