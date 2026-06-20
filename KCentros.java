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

    /**
     * MÉTODO EXATO PARALELO - Com Thread Monitora para Progresso
     */
    public static Resultado exato(FloydWarshall fw, int k, Resultado limiteAproximado) {
        int n = fw.tamanho();
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        AtomicLong melhorRaioGlobal = new AtomicLong(limiteAproximado != null ? limiteAproximado.raio : Long.MAX_VALUE);
        int[] melhoresCentrosGlobal = limiteAproximado != null ? Arrays.copyOf(limiteAproximado.centros, k) : new int[k];
        Object lockEscrita = new Object();

        // Variáveis para a barra de progresso
        BigInteger totalCombinacoes = calcularCombinacao(n, k);
        AtomicLong iteracoesConcluidas = new AtomicLong(0);

        // Thread Monitora: Imprime o progresso sem travar os cálculos
        Thread monitorProgresso = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long concluidas = iteracoesConcluidas.get();
                double pct = totalCombinacoes.equals(BigInteger.ZERO) ? 100.0 : 
                             (concluidas * 100.0) / totalCombinacoes.doubleValue();
                
                System.out.printf("\rProgresso do Método Exato: %.3f%%", pct);
                
                try {
                    Thread.sleep(200); // Atualiza o terminal a cada 200ms
                } catch (InterruptedException e) {
                    break; // Sai do loop quando o processamento terminar
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
                long melhorLocalRaio = melhorRaioGlobal.get();
                long iteracoesLocais = 0; // Contador em lote (Evita gargalo no CPU)

                do {
                    long tetoAtual = melhorRaioGlobal.get();
                    long raio = calcularRaioComPoda(fw, n, comb, tetoAtual);

                    if (raio < tetoAtual) {
                        melhorLocalRaio = raio;
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

                    // Incremento do contador local para a barra de progresso
                    iteracoesLocais++;
                    if (iteracoesLocais >= 100_000) {
                        iteracoesConcluidas.addAndGet(iteracoesLocais);
                        iteracoesLocais = 0;
                    }

                } while (proximaCombinacaoRestrita(comb, n, k));
                
                // Adiciona o remanescente que sobrou na thread
                if (iteracoesLocais > 0) {
                    iteracoesConcluidas.addAndGet(iteracoesLocais);
                }
            }));
        }

        for (Future<?> f : tarefas) {
            try { f.get(); } catch (Exception e) { e.printStackTrace(); }
        }
        
        // Finaliza a barra de progresso de forma limpa
        monitorProgresso.interrupt();
        System.out.println("\rProgresso do Método Exato: 100.000%   ");
        executor.shutdown();

        return new Resultado(melhoresCentrosGlobal, melhorRaioGlobal.get(), "EXATO_PARALELO");
    }

    public static Resultado aproximado(FloydWarshall fw, int k) {
        int n = fw.tamanho();
        long[] minDist = new long[n];
        Arrays.fill(minDist, Long.MAX_VALUE);

        int[] centros = new int[k];
        centros[0] = 0;
        for (int i = 0; i < n; i++) {
            long d = fw.get(i, 0);
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

        return new Resultado(centros, calcularRaioComPoda(fw, n, centros, Long.MAX_VALUE), "APROXIMADO");
    }

    private static long calcularRaioComPoda(FloydWarshall fw, int n, int[] centros, long limiteTeto) {
        long raio = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long minLinha = Long.MAX_VALUE;
            for (int c : centros) {
                long d = fw.get(i, c);
                if (d < minLinha) minLinha = d;
            }
            if (minLinha > raio) raio = minLinha;
            
            if (raio >= limiteTeto) {
                return Long.MAX_VALUE;
            }
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