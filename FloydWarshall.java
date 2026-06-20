import java.io.*;
import java.util.*;

/**
 * Representação do Grafo com Matriz Achatada em 1D (Linear Memory Layout).
 * Maximiza o aproveitamento dos caches L1/L2 eliminando ponteiros redundantes.
 */
public class FloydWarshall {

    public static final long INF = Long.MAX_VALUE / 2;

    private long[]              dist; // Matriz achatada para garantir contiguidade física
    private String[]            vertices;
    private Map<String,Integer> indice;
    private int                 n;

    public FloydWarshall(String caminhoArquivo) throws IOException {
        carregarGrafo(caminhoArquivo);
        executar();
    }

    public FloydWarshall(long[][] matrizDistancias, String[] nomesVertices) {
        this.n        = nomesVertices.length;
        this.vertices = nomesVertices;
        this.indice   = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) indice.put(nomesVertices[i], i);
        
        this.dist = new long[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                this.dist[i * n + j] = matrizDistancias[i][j];
            }
        }
        executar();
    }

    private void carregarGrafo(String caminho) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            int V = Integer.parseInt(st.nextToken());
            int K = Integer.parseInt(st.nextToken());

            indice = new LinkedHashMap<>();
            Map<String, Long> arestas = new LinkedHashMap<>();

            String linha;
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;
                st = new StringTokenizer(linha);
                String u    = st.nextToken();
                String v    = st.nextToken();
                long   peso = Long.parseLong(st.nextToken());
                indice.putIfAbsent(u, indice.size());
                indice.putIfAbsent(v, indice.size());
                arestas.put(chaveCanonica(u, v), peso);
            }

            n        = indice.size();
            vertices = new String[n];
            for (Map.Entry<String,Integer> e : indice.entrySet())
                vertices[e.getValue()] = e.getKey();

            dist = new long[n * n];
            Arrays.fill(dist, INF);
            for (int i = 0; i < n; i++) dist[i * n + i] = 0;

            for (Map.Entry<String,Long> entry : arestas.entrySet()) {
                String[] p = entry.getKey().split("\\|");
                int  u = indice.get(p[0]);
                int  v = indice.get(p[1]);
                long w = entry.getValue();
                dist[u * n + v] = w;
                dist[v * n + u] = w;
            }
        }
    }

    private void executar() {
        // Laço O(N³) otimizado para registradores locais e localidade espacial extrema
        for (int k = 0; k < n; k++) {
            int kn = k * n;
            for (int i = 0; i < n; i++) {
                int in = i * n;
                long dik = dist[in + k];
                if (dik < INF) {
                    for (int j = 0; j < n; j++) {
                        long dkj = dist[kn + j];
                        long novaDist = dik + dkj;
                        if (dkj < INF && novaDist < dist[in + j]) {
                            dist[in + j] = novaDist;
                        }
                    }
                }
            }
        }
    }

    public long get(int i, int j) { 
        return dist[i * n + j]; 
    }

    public int tamanho() { 
        return n; 
    }
    
    public String nomeVertice(int i) { 
        return vertices[i]; 
    }

    public void imprimir() {
        int w = 10;
        System.out.printf("%" + w + "s", "");
        for (String v : vertices) System.out.printf("%" + w + "s", v);
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.printf("%" + w + "s", vertices[i]);
            for (int j = 0; j < n; j++) {
                long d = dist[i * n + j];
                System.out.printf("%" + w + "s", d >= INF ? "INF" : d);
            }
            System.out.println();
        }
    }

    private static String chaveCanonica(String u, String v) {
        return (u.compareTo(v) <= 0) ? u + "|" + v : v + "|" + u;
    }
}