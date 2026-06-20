import java.io.*;
import java.util.*;

public class LeitorPmed {

    private FloydWarshall fw;
    private int           k;
    private int           v;

    public LeitorPmed(String caminho) throws IOException {
        // Uso de try-with-resources para prevenir vazamento de memória do ficheiro
        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            StringTokenizer st = new StringTokenizer(br.readLine());
            v = Integer.parseInt(st.nextToken());
            int e = Integer.parseInt(st.nextToken());
            k = Integer.parseInt(st.nextToken());

            String[] nomes = new String[v];
            for (int i = 0; i < v; i++) nomes[i] = String.valueOf(i + 1);

            long[][] dist = new long[v][v];
            for (long[] row : dist) Arrays.fill(row, FloydWarshall.INF);
            for (int i = 0; i < v; i++) dist[i][i] = 0;

            for (int i = 0; i < e; i++) {
                String linha = br.readLine();
                if (linha == null) break;
                st = new StringTokenizer(linha);
                int  u    = Integer.parseInt(st.nextToken()) - 1;
                int  vv   = Integer.parseInt(st.nextToken()) - 1;
                long peso = Long.parseLong(st.nextToken());
                dist[u][vv] = peso;
                dist[vv][u] = peso;
            }
            fw = new FloydWarshall(dist, nomes);
        }
    }

    public FloydWarshall getFloydWarshall() { return fw; }
    public int getK()                        { return k; }
    public int getV()                        { return v; }
}