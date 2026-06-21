import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;

/**
 * Processamento Paralelo de K-Centros com Barra de Progresso Não-Bloqueante.
 */
public class KCentros {

    public static class Resultado {
        public final int[]  centros;
        public final long   raio;
        public final String metodo;

        Resultado(int[] centros, long raio, String metodo) {
            this.centros = centros;
            this.raio    = raio;
            this.metodo  = metodo;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(metodo).append("] ");
            sb.append("Raio = ").append(raio == FloydWarshall.INF ? "INF" : raio);
            sb.append(" | Centros = {");
            for (int i = 0; i < centros.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(centros[i] + 1);
            }
            sb.append("}");
            return sb.toString();
        }
    }

    // -------------------------------------------------------------------------
    // MÉTODO EXATO PARALELO
    // -------------------------------------------------------------------------

    public static Resultado exato(FloydWarshall fw, int k, Resultado limiteAproximado) {
        int n = fw.tamanho();
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        AtomicLong melhorRaioGlobal = new AtomicLong(
            limiteAproximado != null ? limiteAproximado.raio : Long.MAX_VALUE);
        int[] melhoresCentrosGlobal = limiteAproximado != null
            ? Arrays.copyOf(limiteAproximado.centros, k) : new int[k];
        Object lockEscrita = new Object();

        BigInteger totalCombinacoes = calcularCombinacao(n, k);
        AtomicLong iteracoesConcluidas = new AtomicLong(0);

        Thread monitorProgresso = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long concluidas = iteracoesConcluidas.get();
                double pct = totalCombinacoes.equals(BigInteger.ZERO) ? 100.0
                    : (concluidas * 100.0) / totalCombinacoes.doubleValue();
                System.out.printf("\rProgresso do Método Exato: %.3f%%", pct);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorProgresso.start();

        List<Future<?>> tarefas = new ArrayList<>();

        for (int s = 0; s <= n - k; s++) {
            final int primeiroElemento = s;
            tarefas.add(executor.submit(() -> {
                int[] comb = new int[k];
                comb[0] = primeiroElemento;
                for (int i = 1; i < k; i++) comb[i] = primeiroElemento + i;

                int[] melhorLocalCentros = null;
                long  melhorLocalRaio    = melhorRaioGlobal.get();
                long  iteracoesLocais    = 0;

                do {
                    long tetoAtual = melhorRaioGlobal.get();
                    long raio = calcularRaioComPoda(fw, n, comb, tetoAtual);

                    if (raio < tetoAtual) {
                        melhorLocalRaio    = raio;
                        melhorLocalCentros = Arrays.copyOf(comb, k);

                        while (raio < (tetoAtual = melhorRaioGlobal.get())) {
                            if (melhorRaioGlobal.compareAndSet(tetoAtual, raio)) {
                                synchronized (lockEscrita) {
                                    System.arraycopy(melhorLocalCentros, 0, melhoresCentrosGlobal, 0, k);
                                }
                                break;
                            }
                        }
                    }

                    iteracoesLocais++;
                    if (iteracoesLocais >= 100_000) {
                        iteracoesConcluidas.addAndGet(iteracoesLocais);
                        iteracoesLocais = 0;
                    }

                } while (proximaCombinacaoRestrita(comb, n, k));

                if (iteracoesLocais > 0) iteracoesConcluidas.addAndGet(iteracoesLocais);
            }));
        }

        for (Future<?> f : tarefas) {
            try { f.get(); } catch (Exception e) { e.printStackTrace(); }
        }

        monitorProgresso.interrupt();
        System.out.println("\rProgresso do Método Exato: 100.000%   ");
        executor.shutdown();

        return new Resultado(melhoresCentrosGlobal, melhorRaioGlobal.get(), "EXATO_PARALELO");
    }

    // -------------------------------------------------------------------------
    // MÉTODO APROXIMADO MELHORADO — Multi-Restart Greedy com Perturbação
    // -------------------------------------------------------------------------
    /**
     * Executa o Greedy Farthest-Point com múltiplos reinícios aleatórios para
     * escapar de mínimos locais. O número de reinícios é limitado para garantir
     * viabilidade mesmo em grafos enormes:
     *   - O custo por restart é O(k * n), igual ao greedy original.
     *   - O número de restarts cresce logaritmicamente com n, mas é limitado
     *     a MAX_RESTARTS para que não comprometa o desempenho em instâncias grandes.
     * Retorna o melhor resultado encontrado entre todos os reinícios.
     */
    public static Resultado aproximado(FloydWarshall fw, int k) {
        int n = fw.tamanho();

        // Número de reinícios: escala logaritmicamente mas é limitado.
        // Para n pequeno (≤20): ~10 reinícios. Para n=1000: ~20. Para n=100k: ~30.
        int MAX_RESTARTS = Math.min(50, Math.max(10, (int)(Math.log(n + 1) * 4)));

        Resultado melhor = greedy(fw, k, 0); // Sempre começa do vértice 0 (determinístico)

        // Restarts com sementes aleatórias
        Random rng = new Random(42); // Semente fixa para reprodutibilidade
        for (int r = 1; r < MAX_RESTARTS; r++) {
            int semente = rng.nextInt(n);
            Resultado candidato = greedy(fw, k, semente);
            if (candidato.raio < melhor.raio) {
                melhor = candidato;
            }
        }

        // Fase de refinamento local: tenta trocar cada centro pelo vértice mais distante
        melhor = refinamentoLocal(fw, k, melhor);

        return new Resultado(melhor.centros, melhor.raio, "APROXIMADO_MELHORADO");
    }

    /**
     * Greedy Farthest-Point clássico a partir de um vértice inicial.
     */
    private static Resultado greedy(FloydWarshall fw, int k, int semente) {
        int n = fw.tamanho();
        long[] minDist = new long[n];
        Arrays.fill(minDist, Long.MAX_VALUE);

        int[] centros = new int[k];
        centros[0] = semente;
        for (int i = 0; i < n; i++) {
            long d = fw.get(i, semente);
            if (d < minDist[i]) minDist[i] = d;
        }

        for (int c = 1; c < k; c++) {
            int  proximo = -1;
            long maxDist = -1;
            for (int i = 0; i < n; i++) {
                if (minDist[i] > maxDist) {
                    maxDist = minDist[i];
                    proximo = i;
                }
            }
            centros[c] = proximo;
            for (int i = 0; i < n; i++) {
                long d = fw.get(i, proximo);
                if (d < minDist[i]) minDist[i] = d;
            }
        }

        long raio = calcularRaioComPoda(fw, n, centros, Long.MAX_VALUE);
        return new Resultado(centros, raio, "GREEDY");
    }

    /**
     * Refinamento local: para cada centro, tenta substituí-lo pelo vértice que
     * maximiza a distância mínima a todos os outros centros. Itera até estabilizar.
     * Complexidade por iteração: O(k * n). Número máximo de iterações: k.
     */
    private static Resultado refinamentoLocal(FloydWarshall fw, int k, Resultado inicial) {
        int n = fw.tamanho();
        int[] centros = Arrays.copyOf(inicial.centros, k);
        long  raioAtual = inicial.raio;
        boolean melhorou = true;

        while (melhorou) {
            melhorou = false;
            for (int pos = 0; pos < k; pos++) {
                int centroOriginal = centros[pos];
                // Tenta substituir centros[pos] pelo melhor candidato alternativo
                int melhorSubstituto = centroOriginal;
                long melhorRaio = raioAtual;

                for (int v = 0; v < n; v++) {
                    // Pula vértices que já são centros
                    boolean jaCentro = false;
                    for (int c : centros) if (c == v) { jaCentro = true; break; }
                    if (jaCentro) continue;

                    centros[pos] = v;
                    long novoRaio = calcularRaioComPoda(fw, n, centros, melhorRaio);
                    if (novoRaio < melhorRaio) {
                        melhorRaio = novoRaio;
                        melhorSubstituto = v;
                    }
                }

                centros[pos] = melhorSubstituto;
                if (melhorSubstituto != centroOriginal) {
                    raioAtual = melhorRaio;
                    melhorou  = true;
                }
            }
        }

        return new Resultado(centros, raioAtual, "APROXIMADO_MELHORADO");
    }

    // -------------------------------------------------------------------------
    // UTILITÁRIOS
    // -------------------------------------------------------------------------

    public static long calcularRaioComPoda(FloydWarshall fw, int n, int[] centros, long limiteTeto) {
        long raio = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long minLinha = Long.MAX_VALUE;
            for (int c : centros) {
                long d = fw.get(i, c);
                if (d < minLinha) minLinha = d;
            }
            if (minLinha > raio) raio = minLinha;
            if (raio >= limiteTeto) return Long.MAX_VALUE;
        }
        return raio;
    }

    private static boolean proximaCombinacaoRestrita(int[] comb, int n, int k) {
        int i = k - 1;
        while (i > 0 && comb[i] == n - k + i) i--;
        if (i == 0) return false;
        comb[i]++;
        for (int j = i + 1; j < k; j++) comb[j] = comb[j - 1] + 1;
        return true;
    }

    public static BigInteger calcularCombinacao(int n, int k) {
        if (k > n - k) k = n - k;
        BigInteger res = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            res = res.multiply(BigInteger.valueOf(n - i + 1)).divide(BigInteger.valueOf(i));
        }
        return res;
    }
}
