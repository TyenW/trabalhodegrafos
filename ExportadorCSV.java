import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Exporta resultados para CSV de forma incremental.
 * Cada resultado é gravado imediatamente após sua conclusão.
 */
public class ExportadorCSV {

    private static final String[] CABECALHO = {
        "Timestamp",
        "NomeGrafo",
        "NumVertices",
        "NumK",
        "Metodo",
        "Raio",
        "Centros",
        "TempoRealMs",
        "TempoRealFormatado",
        "EstimativaSegundos",
        "EstimativaFormatada",
        "DiferencaEstimativaRealS",
        "FatorPodaEstimado",
        "TotalCombinacoes",
        "NumCores",
        "VelocidadeCalibradadOpsS",
        "GapAproxVsExatoAbsoluto",
        "GapAproxVsExatoPorcentagem"
    };

    private final String                 caminho;
    private final DateTimeFormatter      fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExportadorCSV(String caminho) throws IOException {
        this.caminho = caminho;
        File f = new File(caminho);
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                pw.println(String.join(",", CABECALHO));
            }
            System.out.printf("  [CSV] Arquivo criado: %s%n", f.getAbsolutePath());
        } else {
            System.out.printf("  [CSV] Anexando ao arquivo existente: %s%n", f.getAbsolutePath());
        }
    }

    public synchronized void registrar(
            String             nomeGrafo,
            int                numVertices,
            int                k,
            KCentros.Resultado resultado,
            long               tempoRealMs,
            EstimadorTempo.Estimativa estimativa,   // pode ser null (aproximado sem análise prévia)
            Long               gapAbsoluto,
            Double             gapPorcentagem) {

        try (PrintWriter pw = new PrintWriter(new FileWriter(caminho, true))) {

            String ts        = LocalDateTime.now().format(fmt);
            String raio      = resultado.raio >= FloydWarshall.INF ? "INF" : String.valueOf(resultado.raio);
            String centros   = centrosStr(resultado.centros);
            String tempoFmt  = Main.formatSegundos(tempoRealMs / 1000.0);

            // Estimativa (pode ser null quando o aproximado é gravado antes da análise exata)
            String estimSeg  = estimativa != null ? String.format("%.3f", estimativa.segundosEstimados) : "";
            String estimFmt  = estimativa != null ? Main.formatSegundos(estimativa.segundosEstimados)   : "";
            String difStr    = (estimativa != null)
                ? String.format("%.3f", estimativa.segundosEstimados - tempoRealMs / 1000.0)
                : "";
            String fatorPoda = estimativa != null ? String.format("%.6f", estimativa.fatorPoda) : "";
            String totalComb = estimativa != null
                ? EstimadorTempo.formatarNumeroGrande(estimativa.totalCombinacoes) : "";
            String cores     = estimativa != null ? String.valueOf(estimativa.cores) : "";
            String vel       = estimativa != null ? String.format("%.0f", estimativa.opsCalibradas) : "";

            String gapAbs    = gapAbsoluto    != null ? String.valueOf(gapAbsoluto)              : "";
            String gapPct    = gapPorcentagem != null ? String.format("%.4f", gapPorcentagem)    : "";

            pw.println(
                esc(ts)          + "," +
                esc(nomeGrafo)   + "," +
                numVertices      + "," +
                k                + "," +
                esc(resultado.metodo) + "," +
                esc(raio)        + "," +
                esc(centros)     + "," +
                tempoRealMs      + "," +
                esc(tempoFmt)    + "," +
                esc(estimSeg)    + "," +
                esc(estimFmt)    + "," +
                esc(difStr)      + "," +
                esc(fatorPoda)   + "," +
                esc(totalComb)   + "," +
                esc(cores)       + "," +
                esc(vel)         + "," +
                esc(gapAbs)      + "," +
                esc(gapPct)
            );

        } catch (IOException ex) {
            System.err.printf("  [CSV] ERRO ao gravar resultado de '%s': %s%n", nomeGrafo, ex.getMessage());
        }
    }

    private static String centrosStr(int[] centros) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < centros.length; i++) {
            if (i > 0) sb.append(";");
            sb.append(centros[i] + 1);
        }
        return sb.toString();
    }

    /** Envolve em aspas duplas se contiver vírgula, aspas ou quebra de linha. */
    private static String esc(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    public String getCaminho() { return caminho; }
}
