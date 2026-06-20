import java.util.*;

/**
 * Resolve o Problema dos k-Centros por dois metodos:
 *
 * 1. EXATO      - forca bruta C(n,k). Otimo garantido. Apenas para n pequeno.
 * 2. APROXIMADO - Greedy Farthest-Point. Raio <= 2x otimo. Rapido para n grande.
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

    // METODO EXATO - O(C(n,k) x n x k)
    public static Resultado exato(FloydWarshall fw, int k) {
        int n = fw.tamanho();
        long  melhorRaio      = Long.MAX_VALUE;
        int[] melhoresCentros = null;

        // 1. Calcular o total de combinações possíveis para o progresso
        long totalCombinacoes = calcularCombinacao(n, k);
        long iteracaoAtual = 0;

        int[] comb = new int[k];
        inicializar(comb, k);
        
        // Mensagem inicial de progresso
        System.out.print("Progresso do Método Exato: 0.00%\r");

        do {
            long raio = calcularRaio(fw, n, comb);
            if (raio < melhorRaio) {
                melhorRaio      = raio;
                melhoresCentros = Arrays.copyOf(comb, k);
            }
            
            // 2. Incrementar o contador
            iteracaoAtual++;

            // 3. Atualizar o terminal a cada 100 mil iterações ou no final
            if (iteracaoAtual % 100000 == 0 || iteracaoAtual == totalCombinacoes) {
                double porcentagem = ((double) iteracaoAtual / totalCombinacoes) * 100;
                System.out.printf("Progresso do Método Exato: %.2f%% (%d/%d)\r", 
                    porcentagem, iteracaoAtual, totalCombinacoes);
            }
            
        } while (proximaCombinacao(comb, n, k));
        
        // Quebra de linha ao finalizar para não sobrescrever a barra de progresso
        System.out.println();

        return new Resultado(melhoresCentros, melhorRaio, "EXATO");
    }

    // METODO APROXIMADO - Greedy Farthest-Point, O(k x n)
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

        return new Resultado(centros, calcularRaio(fw, n, centros), "APROXIMADO");
    }

    public static long calcularRaio(FloydWarshall fw, int n, int[] centros) {
        long raio = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long minLinha = Long.MAX_VALUE;
            for (int c : centros) {
                long d = fw.get(i, c);
                if (d < minLinha) minLinha = d;
            }
            if (minLinha > raio) raio = minLinha;
        }
        return raio;
    }

    private static void inicializar(int[] comb, int k) {
        for (int i = 0; i < k; i++) comb[i] = i;
    }

    private static boolean proximaCombinacao(int[] comb, int n, int k) {
        int i = k - 1;
        while (i >= 0 && comb[i] == n - k + i) i--;
        if (i < 0) return false;
        comb[i]++;
        for (int j = i + 1; j < k; j++) comb[j] = comb[j - 1] + 1;
        return true;
    }
    
    // Método auxiliar para o cálculo do total de combinações
    private static long calcularCombinacao(int n, int k) {
        if (k > n - k) {
            k = n - k;
        }
        long res = 1;
        for (int i = 1; i <= k; i++) {
            res = res * (n - i + 1) / i;
        }
        return res;
    }
}