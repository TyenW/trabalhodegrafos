import java.io.*;
import java.util.*;

/**
 * Main - Problema dos k-Centros
 *
 * Uso:
 *   java Main <arquivo> [pmed | k]
 *
 * Exemplos:
 *   java Main grafo.txt 2        <- formato proprio, k=2
 *   java Main pmed1.txt pmed     <- formato OR-Library
 */
public class Main {

    static final int LIMITE_EXATO = 20;

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.err.println("Uso: java Main <arquivo> [pmed | k]");
            System.exit(1);
        }

        String  arquivo = args[0];
        boolean isPmed  = args.length > 1 && args[1].equalsIgnoreCase("pmed");

        FloydWarshall fw;
        int k;

        if (isPmed) {
            LeitorPmed lp = new LeitorPmed(arquivo);
            fw = lp.getFloydWarshall();
            k  = lp.getK();
            System.out.printf("Instancia OR-Library: %d vertices, k=%d%n%n", lp.getV(), k);
        } else {
            fw = new FloydWarshall(arquivo);
            k  = (args.length > 1) ? Integer.parseInt(args[1]) : 2;
            System.out.printf("Grafo proprio: %d vertices, k=%d%n%n", fw.tamanho(), k);
        }

        int n = fw.tamanho();

        if (n <= 20) {
            System.out.println("Matriz de distancias (Floyd-Warshall):");
            fw.imprimir();
            System.out.println();
        }

        System.out.println("-----------------------------------------");

        // APROXIMADO (sempre roda)
        long t0 = System.currentTimeMillis();
        KCentros.Resultado aprox = KCentros.aproximado(fw, k);
        long tAprox = System.currentTimeMillis() - t0;
        System.out.println(aprox);
        System.out.printf("  Tempo: %d ms%n", tAprox);

        // EXATO (so para instancias pequenas)
        if (n <= LIMITE_EXATO) {
            t0 = System.currentTimeMillis();
            KCentros.Resultado exato = KCentros.exato(fw, k);
            long tExato = System.currentTimeMillis() - t0;
            System.out.println(exato);
            System.out.printf("  Tempo: %d ms%n", tExato);

            System.out.println("-----------------------------------------");
            long gap = aprox.raio - exato.raio;
            System.out.printf("Gap aproximado vs exato: %+d (%.1f%%)%n",
                gap, exato.raio == 0 ? 0.0 : 100.0 * gap / exato.raio);
        } else {
            System.out.println("-----------------------------------------");
            System.out.printf("(Instancia grande - metodo exato omitido; n=%d)%n", n);
        }
        System.out.println("-----------------------------------------");
    }
}
