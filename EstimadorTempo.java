import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estimador de tempo de execução para o método exato de K-Centros.
 *
 * Estratégia em camadas:
 *  1. Benchmark real de calibração (micro-benchmark no início)
 *  2. Modelo analítico baseado em características do grafo (n, k, densidade, poda estimada)
 *  3. Refinamento dinâmico durante a execução (taxa observada de combinações/s)
 */
public class EstimadorTempo {

    // -------------------------------------------------------------------------
    // Calibração: mede as operações reais por segundo neste hardware
    // -------------------------------------------------------------------------
    private static volatile double OPS_POR_SEGUNDO_CALIBRADO = -1.0;
    private static final Object LOCK_CALIB = new Object();

    /**
     * Executa um micro-benchmark para medir a velocidade real da CPU neste sistema.
     * É chamado uma vez e o resultado é cacheado.
     * Simula exatamente o inner-loop do método exato: calcularRaioComPoda.
     */
    public static double calibrar(FloydWarshall fw) {
        synchronized (LOCK_CALIB) {
            if (OPS_POR_SEGUNDO_CALIBRADO > 0) return OPS_POR_SEGUNDO_CALIBRADO;

            int n = fw.tamanho();
            int k = Math.min(5, Math.max(2, n / 10)); // k representativo

            // Cria uma combinação de centros de teste
            int[] centrosTeste = new int[k];
            for (int i = 0; i < k; i++) centrosTeste[i] = i;

            // Aquece a JVM (descartado)
            long warmupOps = 0;
            long warmupInicio = System.nanoTime();
            while (System.nanoTime() - warmupInicio < 100_000_000L) { // 100ms
                KCentros.calcularRaioComPoda(fw, n, centrosTeste, Long.MAX_VALUE);
                warmupOps++;
            }

            // Medição real
            long medicaoOps = 0;
            long inicio = System.nanoTime();
            long duracao = 300_000_000L; // 300ms
            while (System.nanoTime() - inicio < duracao) {
                KCentros.calcularRaioComPoda(fw, n, centrosTeste, Long.MAX_VALUE);
                medicaoOps++;
            }
            long fim = System.nanoTime();

            double segundosMedidos = (fim - inicio) / 1e9;
            // Operações/s = avaliações de combinação por segundo
            OPS_POR_SEGUNDO_CALIBRADO = medicaoOps / segundosMedidos;
            return OPS_POR_SEGUNDO_CALIBRADO;
        }
    }

    // -------------------------------------------------------------------------
    // Estimativa estática (antes de rodar)
    // -------------------------------------------------------------------------

    /**
     * Estima o tempo de execução em segundos com base nas características do grafo.
     *
     * @param fw     Grafo já processado pelo Floyd-Warshall
     * @param k      Número de centros
     * @param aprox  Resultado aproximado (usado para estimar poda)
     * @return       Estimativa em segundos (pior caso mitigado pela poda)
     */
    public static Estimativa estimar(FloydWarshall fw, int k, KCentros.Resultado aprox) {
        int n = fw.tamanho();
        int cores = Runtime.getRuntime().availableProcessors();
        BigInteger totalCombinacoes = KCentros.calcularCombinacao(n, k);

        // Calibração real da velocidade
        double opsCalibradas = calibrar(fw);

        // Fator de poda: estima quantas combinações serão podadas.
        // A poda é mais eficaz quando o raio aproximado é próximo do ótimo.
        // Heurística: densidade do grafo afeta a poda.
        double fatorPoda = estimarFatorPoda(n, k, fw, aprox);

        // Combinações efetivas após poda
        double combinacoesEfetivas = totalCombinacoes.doubleValue() * fatorPoda;

        // Tempo bruto sem paralelismo
        double segundosBruto = combinacoesEfetivas / opsCalibradas;

        // Com paralelismo (eficiência ~85% para muitas threads)
        double eficienciaParalela = Math.min(0.85, 0.95 - 0.1 * Math.log10(Math.max(1, cores)));
        double segundosParalelo = segundosBruto / (cores * eficienciaParalela);

        // Pior caso (sem poda), para referência
        double segundosPiorCaso = totalCombinacoes.doubleValue() / (opsCalibradas * cores * eficienciaParalela);

        return new Estimativa(
            segundosParalelo,
            segundosPiorCaso,
            fatorPoda,
            opsCalibradas,
            cores,
            totalCombinacoes
        );
    }

    /**
     * Estima o fator de poda (0..1) com base em características do grafo e na
     * qualidade da solução aproximada.
     *
     * Lógica:
     * - Grafos densos têm caminhos curtos → raio do ótimo próximo do aprox → poda fraca
     * - k grande relativo a n → muitas soluções boas → poda forte
     * - Raio aprox pequeno relativo ao raio médio do grafo → poda forte
     */
    private static double estimarFatorPoda(int n, int k, FloydWarshall fw, KCentros.Resultado aprox) {
        // Calcula o raio médio do grafo (amostra para eficiência)
        int amostras = Math.min(n, 50);
        long somaRaioMedio = 0;
        int count = 0;
        Random rng = new Random(1);
        for (int t = 0; t < amostras; t++) {
            int i = rng.nextInt(n);
            long minDist = Long.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    long d = fw.get(i, j);
                    if (d < FloydWarshall.INF && d < minDist) minDist = d;
                }
            }
            if (minDist < Long.MAX_VALUE) { somaRaioMedio += minDist; count++; }
        }
        double raioMedio = count > 0 ? (double) somaRaioMedio / count : 1.0;

        // Razão raio aproximado / raio médio: quanto menor, mais poda
        double raioAprox = (aprox != null && aprox.raio < FloydWarshall.INF) ? aprox.raio : raioMedio * 10;
        double razaoRaio = raioAprox / Math.max(1.0, raioMedio);

        // Fração k/n: quanto maior, mais soluções próximas do ótimo → mais poda
        double fracaoK = (double) k / n;

        // Modelo empírico calibrado contra instâncias típicas OR-Library
        // fatorBase: da razão raio (normalizada) — poda aumenta quando razão é menor
        double fatorBase = Math.min(1.0, 0.05 + 0.95 * (razaoRaio / (razaoRaio + k)));

        // Ajuste pela fração k/n: k grande → mais poda
        double ajusteK = Math.pow(1.0 - Math.min(0.9, fracaoK * 3), 0.5);

        return Math.max(0.001, Math.min(1.0, fatorBase * ajusteK));
    }

    // -------------------------------------------------------------------------
    // Estimativa dinâmica (durante a execução)
    // -------------------------------------------------------------------------

    /**
     * Calcula a estimativa refinada de tempo restante com base na taxa observada.
     */
    public static double estimarTempoRestante(
            BigInteger totalCombinacoes,
            long combinacoesConcluidas,
            long tempoDecorridoMs) {

        if (combinacoesConcluidas <= 0 || tempoDecorridoMs <= 0) return Double.POSITIVE_INFINITY;

        double taxaObservada = (double) combinacoesConcluidas / (tempoDecorridoMs / 1000.0);
        double restantes = totalCombinacoes.doubleValue() - combinacoesConcluidas;
        return restantes / taxaObservada;
    }

    // -------------------------------------------------------------------------
    // Classe resultado da estimativa
    // -------------------------------------------------------------------------

    public static class Estimativa {
        public final double     segundosEstimados;   // Com poda e paralelismo
        public final double     segundosPiorCaso;    // Sem poda
        public final double     fatorPoda;
        public final double     opsCalibradas;
        public final int        cores;
        public final BigInteger totalCombinacoes;

        Estimativa(double seg, double piorCaso, double poda, double ops, int cores, BigInteger total) {
            this.segundosEstimados = seg;
            this.segundosPiorCaso  = piorCaso;
            this.fatorPoda         = poda;
            this.opsCalibradas     = ops;
            this.cores             = cores;
            this.totalCombinacoes  = total;
        }

        @Override
        public String toString() {
            return String.format(
                "Estimativa: %s (c/ poda %.1f%%) | Pior caso: %s | Combinações: %s | Cores: %d | Vel: %.1fM comb/s",
                formatarTempo(segundosEstimados),
                (1.0 - fatorPoda) * 100,
                formatarTempo(segundosPiorCaso),
                formatarNumeroGrande(totalCombinacoes),
                cores,
                opsCalibradas / 1_000_000
            );
        }
    }

    // -------------------------------------------------------------------------
    // Formatação
    // -------------------------------------------------------------------------

    public static String formatarTempo(double segundos) {
        if (segundos == Double.POSITIVE_INFINITY || Double.isNaN(segundos)) return "Infinito";
        if (segundos < 0.001) return "< 1 ms";
        if (segundos < 1)    return String.format("%.0f ms", segundos * 1000);
        if (segundos < 60)   return String.format("%.1f segundos", segundos);
        if (segundos < 3600) return String.format("%.1f minutos", segundos / 60);
        if (segundos < 86400) return String.format("%.1f horas", segundos / 3600);
        return String.format("%.1f dias", segundos / 86400);
    }

    public static String formatarNumeroGrande(BigInteger num) {
        String str = num.toString();
        if (str.length() > 6)
            return String.format("%s.%.2s x 10^%d", str.charAt(0), str.substring(1), str.length() - 1);
        return str;
    }
}
