import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/**
 * Exporta resultados de execução para CSV de forma incremental.
 * Cada resultado é gravado imediatamente após a conclusão de cada grafo,
 * garantindo que interrupções não causem perda de dados.
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
        "TempoExecucaoMs",
        "EstimativaTempo",
        "FatorPoda",
        "TotalCombinacoes",
        "NumCores",
        "VelocidadeOpsS",
        "GapAproximadoVsExato",
        "GapPorcentagem"
    };

    private final String caminhoArquivo;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExportadorCSV(String caminhoArquivo) throws IOException {
        this.caminhoArquivo = caminhoArquivo;
        // Cria o arquivo com cabeçalho se ainda não existir
        File f = new File(caminhoArquivo);
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                pw.println(String.join(",", CABECALHO));
            }
            System.out.printf("  [CSV] Arquivo criado: %s%n", f.getAbsolutePath());
        } else {
            System.out.printf("  [CSV] Anexando ao arquivo existente: %s%n", f.getAbsolutePath());
        }
    }

    /**
     * Registra um resultado de execução. Chamado imediatamente após cada grafo.
     */
    public synchronized void registrar(
            String nomeGrafo,
            int numVertices,
            int k,
            KCentros.Resultado resultado,
            long tempoMs,
            EstimadorTempo.Estimativa estimativa,
            Long gapAbsoluto,      // null se não aplicável
            Double gapPorcentagem  // null se não aplicável
    ) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(caminhoArquivo, true))) {
            // Timestamp
            String ts = LocalDateTime.now().format(fmt);

            // Centros: lista separada por ;
            StringBuilder centros = new StringBuilder();
            for (int i = 0; i < resultado.centros.length; i++) {
                if (i > 0) centros.append(";");
                centros.append(resultado.centros[i] + 1);
            }

            // Raio
            String raioStr = (resultado.raio >= FloydWarshall.INF) ? "INF" : String.valueOf(resultado.raio);

            // Estimativa
            String estimStr = (estimativa != null)
                ? EstimadorTempo.formatarTempo(estimativa.segundosEstimados) : "N/A";
            String fatorPodaStr = (estimativa != null)
                ? String.format("%.4f", estimativa.fatorPoda) : "N/A";
            String totalCombStr = (estimativa != null)
                ? EstimadorTempo.formatarNumeroGrande(estimativa.totalCombinacoes) : "N/A";
            String coresStr = (estimativa != null)
                ? String.valueOf(estimativa.cores) : "N/A";
            String velStr = (estimativa != null)
                ? String.format("%.0f", estimativa.opsCalibradas) : "N/A";

            // Gap
            String gapAbsStr = (gapAbsoluto != null) ? String.valueOf(gapAbsoluto) : "N/A";
            String gapPctStr = (gapPorcentagem != null) ? String.format("%.2f%%", gapPorcentagem) : "N/A";

            String linha = csvEscape(ts) + ","
                + csvEscape(nomeGrafo) + ","
                + numVertices + ","
                + k + ","
                + csvEscape(resultado.metodo) + ","
                + csvEscape(raioStr) + ","
                + csvEscape("\"" + centros + "\"") + ","
                + tempoMs + ","
                + csvEscape(estimStr) + ","
                + csvEscape(fatorPodaStr) + ","
                + csvEscape(totalCombStr) + ","
                + csvEscape(coresStr) + ","
                + csvEscape(velStr) + ","
                + csvEscape(gapAbsStr) + ","
                + csvEscape(gapPctStr);

            pw.println(linha);
            System.out.printf("  [CSV] Resultado gravado para '%s' [%s]%n", nomeGrafo, resultado.metodo);

        } catch (IOException e) {
            System.err.println("  [CSV] ERRO ao gravar resultado: " + e.getMessage());
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        // Se já está entre aspas duplas, retorna como está
        if (s.startsWith("\"") && s.endsWith("\"")) return s;
        // Escapa vírgulas e aspas
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public String getCaminhoArquivo() { return caminhoArquivo; }
}
