import java.io.*;
import java.util.*;

/**
 * Le um grafo no formato:
 *   V K
 *   u1 v1 peso1
 *   ...
 * e executa Floyd-Warshall para calcular todas as distancias minimas.
 *
 * Regras:
 *  - Grafo NAO direcionado
 *  - Last-write-wins: se "u v p1" e depois "v u p2" aparecem, vale p2
 */
public class FloydWarshall {

    public static final long INF = Long.MAX_VALUE / 2;

    private long[][]            dist;
    private String[]            vertices;
    private Map<String,Integer> indice;
    private int                 n;

    // Construtor: le arquivo e executa o algoritmo
    public FloydWarshall(String caminhoArquivo) throws IOException {
        carregarGrafo(caminhoArquivo);
        executar();
    }

    // Construtor alternativo: recebe a matriz pronta (para pmed)
    public FloydWarshall(long[][] matrizDistancias, String[] nomesVertices) {
        this.n        = nomesVertices.length;
        this.vertices = nomesVertices;
        this.indice   = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) indice.put(nomesVertices[i], i);
        dist = new long[n][n];
        for (int i = 0; i < n; i++)
            dist[i] = Arrays.copyOf(matrizDistancias[i], n);
        executar();
    }

    private void carregarGrafo(String caminho) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(caminho));
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
        br.close();

        n        = indice.size();
        vertices = new String[n];
        for (Map.Entry<String,Integer> e : indice.entrySet())
            vertices[e.getValue()] = e.getKey();

        dist = new long[n][n];
        for (long[] row : dist) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) dist[i][i] = 0;

        for (Map.Entry<String,Long> entry : arestas.entrySet()) {
            String[] p = entry.getKey().split("\\|");
            int  u = indice.get(p[0]);
            int  v = indice.get(p[1]);
            long w = entry.getValue();
            dist[u][v] = w;
            dist[v][u] = w;
        }
    }

    private void executar() {
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                if (dist[i][k] < INF)
                    for (int j = 0; j < n; j++)
                        if (dist[k][j] < INF && dist[i][k] + dist[k][j] < dist[i][j])
                            dist[i][j] = dist[i][k] + dist[k][j];
    }

    public long   get(int i, int j)        { return dist[i][j]; }
    public long[] getLinha(int i)          { return Arrays.copyOf(dist[i], n); }
    public long[] getColuna(int j)         {
        long[] col = new long[n];
        for (int i = 0; i < n; i++) col[i] = dist[i][j];
        return col;
    }
    public long   get(String u, String v)  { return dist[indice.get(u)][indice.get(v)]; }
    public long[] getLinha(String u)       { return getLinha(indice.get(u)); }
    public long[] getColuna(String v)      { return getColuna(indice.get(v)); }

    public int      tamanho()              { return n; }
    public String   nomeVertice(int i)     { return vertices[i]; }
    public int      indiceVertice(String s){ return indice.get(s); }
    public long[][] getMatriz()            {
        long[][] c = new long[n][n];
        for (int i = 0; i < n; i++) c[i] = Arrays.copyOf(dist[i], n);
        return c;
    }

    public void imprimir() {
        int w = 10;
        System.out.printf("%" + w + "s", "");
        for (String v : vertices) System.out.printf("%" + w + "s", v);
        System.out.println();
        for (int i = 0; i < n; i++) {
            System.out.printf("%" + w + "s", vertices[i]);
            for (int j = 0; j < n; j++)
                System.out.printf("%" + w + "s", dist[i][j] >= INF ? "INF" : dist[i][j]);
            System.out.println();
        }
    }

    private static String chaveCanonica(String u, String v) {
        return (u.compareTo(v) <= 0) ? u + "|" + v : v + "|" + u;
    }
}
