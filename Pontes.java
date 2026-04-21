import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Pontes {
    private static int tempoTarjan;
    private static final String BASE_PREFIX = "grafos";
    private static final String GRAFO_PREFIX = "grafo";

    private static int vertices;
    private static int arestasCabecalho;
    private static List<Aresta> arestas;
    private static List<Vizinho>[] adj;

    private static class Aresta {
        int u;
        int v;

        Aresta(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    private static class Vizinho {
        int to;
        int edgeId;

        Vizinho(int to, int edgeId) {
            this.to = to;
            this.edgeId = edgeId;
        }
    }

    @SuppressWarnings("unchecked")
    private static void carregarGrafo(File arquivo) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(arquivo.toPath())) {
            String cabecalho = reader.readLine();
            if (cabecalho == null || cabecalho.trim().isEmpty()) {
                throw new IOException("arquivo vazio");
            }

            String[] partes = cabecalho.trim().split("\\s+");
            if (partes.length < 2) {
                throw new IOException("cabecalho invalido: esperado 'N M'");
            }

            vertices = Integer.parseInt(partes[0]);
            arestasCabecalho = Integer.parseInt(partes[1]);
            if (vertices <= 0) {
                throw new IOException("numero de vertices invalido: " + vertices);
            }

            arestas = new ArrayList<>(Math.max(1, arestasCabecalho));
            adj = new ArrayList[vertices + 1];
            for (int i = 1; i <= vertices; i++) {
                adj[i] = new ArrayList<>();
            }

            String linha;
            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) {
                    continue;
                }

                String[] uv;
                if (linha.contains("-")) {
                    uv = linha.split("-");
                } else {
                    uv = linha.split("\\s+");
                }
                if (uv.length != 2) {
                    throw new IOException("aresta invalida: '" + linha + "'");
                }

                int u = Integer.parseInt(uv[0].trim());
                int v = Integer.parseInt(uv[1].trim());
                if (u < 1 || u > vertices || v < 1 || v > vertices) {
                    throw new IOException("aresta fora do intervalo de vertices: '" + linha + "'");
                }

                int edgeId = arestas.size();
                arestas.add(new Aresta(u, v));
                adj[u].add(new Vizinho(v, edgeId));
                if (u != v) {
                    adj[v].add(new Vizinho(u, edgeId));
                }
            }
        }
    }

    private static int contarComponentesIgnorandoAresta(int edgeIgnorada) {
        boolean[] visitado = new boolean[vertices + 1];
        int componentes = 0;

        for (int inicio = 1; inicio <= vertices; inicio++) {
            if (visitado[inicio] || adj[inicio].isEmpty()) {
                continue;
            }

            componentes++;
            ArrayDeque<Integer> pilha = new ArrayDeque<>();
            pilha.push(inicio);
            visitado[inicio] = true;

            while (!pilha.isEmpty()) {
                int u = pilha.pop();
                for (Vizinho viz : adj[u]) {
                    if (viz.edgeId == edgeIgnorada) {
                        continue;
                    }
                    int v = viz.to;
                    if (!visitado[v]) {
                        visitado[v] = true;
                        pilha.push(v);
                    }
                }
            }
        }

        return componentes;
    }

    public static int naive() {
        int componentesOriginal = contarComponentesIgnorandoAresta(-1);
        int totalPontes = 0;

        for (int edgeId = 0; edgeId < arestas.size(); edgeId++) {
            Aresta a = arestas.get(edgeId);
            if (a.u == a.v) {
                continue;
            }

            int componentesSemAresta = contarComponentesIgnorandoAresta(edgeId);
            if (componentesSemAresta > componentesOriginal) {
                totalPontes++;
            }
        }

        return totalPontes;
    }

    private static List<Integer> tarjanIterativo() {
        int[] td = new int[vertices + 1];
        int[] low = new int[vertices + 1];
        int[] parent = new int[vertices + 1];
        int[] parentEdge = new int[vertices + 1];
        int[] itIndex = new int[vertices + 1];
        int[] pilha = new int[vertices + 1];
        List<Integer> pontes = new ArrayList<>();

        Arrays.fill(parentEdge, -1);
        tempoTarjan = 0;

        for (int inicio = 1; inicio <= vertices; inicio++) {
            if (td[inicio] != 0) {
                continue;
            }

            int topo = 0;
            pilha[topo++] = inicio;
            parent[inicio] = 0;

            while (topo > 0) {
                int u = pilha[topo - 1];
                if (td[u] == 0) {
                    td[u] = low[u] = ++tempoTarjan;
                    itIndex[u] = 0;
                }

                if (itIndex[u] < adj[u].size()) {
                    Vizinho viz = adj[u].get(itIndex[u]++);
                    int v = viz.to;
                    int edgeId = viz.edgeId;

                    if (edgeId == parentEdge[u]) {
                        continue;
                    }

                    if (td[v] == 0) {
                        parent[v] = u;
                        parentEdge[v] = edgeId;
                        pilha[topo++] = v;
                    } else {
                        low[u] = Math.min(low[u], td[v]);
                    }
                } else {
                    topo--;
                    int p = parent[u];
                    if (p != 0) {
                        low[p] = Math.min(low[p], low[u]);
                        if (low[u] > td[p]) {
                            pontes.add(parentEdge[u]);
                        }
                    }
                }
            }
        }

        return pontes;
    }

    public static int tarjan(boolean imprimirArestas) {
        List<Integer> idsPontes = tarjanIterativo();

        System.out.println("--- Relatorio de Pontes ---");
        if (idsPontes.isEmpty()) {
            System.out.println("Nenhuma ponte encontrada.");
            return 0;
        }

        if (imprimirArestas) {
            Set<Integer> vistos = new HashSet<>();
            for (int id : idsPontes) {
                if (!vistos.add(id)) {
                    continue;
                }
                Aresta a = arestas.get(id);
                System.out.println("Aresta (" + a.u + ", " + a.v + ") e uma ponte!");
            }
        } else {
            System.out.println("Total de pontes: " + idsPontes.size());
        }

        return idsPontes.size();
    }

    private static List<File> listarPastasBase(File raiz) {
        File[] pastas = raiz.listFiles();
        List<File> resultado = new ArrayList<>();
        if (pastas == null) {
            return resultado;
        }

        for (File pasta : pastas) {
            if (!pasta.isDirectory()) {
                continue;
            }
            String nome = pasta.getName().toLowerCase();
            if (nome.startsWith(BASE_PREFIX)
                && (nome.endsWith("euleriano") || nome.endsWith("semieuleriano") || nome.endsWith("naoeuleriano"))) {
                resultado.add(pasta);
            }
        }

        resultado.sort(Comparator.comparing(File::getName));
        return resultado;
    }

    private static List<File> listarSubpastasGrafo(File pastaBase) {
        File[] pastas = pastaBase.listFiles();
        List<File> resultado = new ArrayList<>();
        if (pastas == null) {
            return resultado;
        }

        for (File pasta : pastas) {
            if (!pasta.isDirectory()) {
                continue;
            }
            if (pasta.getName().toLowerCase().startsWith(GRAFO_PREFIX)) {
                resultado.add(pasta);
            }
        }

        resultado.sort(Comparator.comparing(File::getName));
        return resultado;
    }

    private static int lerOpcao(Scanner scanner, int min, int max) {
        while (true) {
            String linha = scanner.nextLine().trim();
            try {
                int valor = Integer.parseInt(linha);
                if (valor < min || valor > max) {
                    System.out.print("Opcao invalida. Digite novamente: ");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero: ");
            }
        }
    }

    private static List<File> escolherPastasBase(Scanner scanner, File raiz) {
        List<File> bases = listarPastasBase(raiz);
        List<File> selecionadas = new ArrayList<>();
        if (bases.isEmpty()) {
            return selecionadas;
        }

        System.out.println("Escolha as pastas base para verificar:");
        for (int i = 0; i < bases.size(); i++) {
            System.out.println((i + 1) + ". " + bases.get(i).getName());
        }
        System.out.println("0. Todas as pastas");
        System.out.print("Digite os numeros separados por espaco (ex: 1 3 4): ");

        String linha = scanner.nextLine().trim();
        if (linha.isEmpty() || linha.equals("0")) {
            selecionadas.addAll(bases);
            return selecionadas;
        }

        String[] partes = linha.split("\\s+");
        boolean[] usado = new boolean[bases.size() + 1];
        for (String parte : partes) {
            try {
                int indice = Integer.parseInt(parte);
                if (indice >= 1 && indice <= bases.size() && !usado[indice]) {
                    selecionadas.add(bases.get(indice - 1));
                    usado[indice] = true;
                }
            } catch (NumberFormatException e) {
                // Ignora token invalido.
            }
        }

        if (selecionadas.isEmpty()) {
            System.out.println("Nenhuma opcao valida informada. Serao usadas todas as pastas.");
            selecionadas.addAll(bases);
        }

        return selecionadas;
    }

    private static int escolherAlgoritmo(Scanner scanner) {
        System.out.println("Escolha o algoritmo para detectar pontes:");
        System.out.println("1. Tarjan (recomendado)");
        System.out.println("2. Naive (lento)");
        System.out.print("Opcao: ");
        return lerOpcao(scanner, 1, 2);
    }

    private static boolean escolherImpressaoDetalhada(Scanner scanner) {
        System.out.println("Deseja listar cada ponte encontrada?");
        System.out.println("1. Sim");
        System.out.println("2. Nao (apenas total)");
        System.out.print("Opcao: ");
        return lerOpcao(scanner, 1, 2) == 1;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        File raiz = new File(".");

        List<File> pastasBase = escolherPastasBase(scanner, raiz);
        if (pastasBase.isEmpty()) {
            System.out.println("Nenhuma pasta base no formato esperado foi encontrada.");
            return;
        }

        int algoritmo = escolherAlgoritmo(scanner);
        boolean detalharPontes = escolherImpressaoDetalhada(scanner);

        int totalProcessados = 0;
        int totalComErro = 0;

        for (File pastaBase : pastasBase) {
            List<File> subpastas = listarSubpastasGrafo(pastaBase);
            if (subpastas.isEmpty()) {
                System.out.println("Nenhuma subpasta grafoN foi encontrada em " + pastaBase.getName() + ".");
                continue;
            }

            System.out.println("\n=== Pasta base: " + pastaBase.getName() + " ===");
            for (File subpasta : subpastas) {
                File arquivo = new File(subpasta, "grafo.txt");
                if (!arquivo.exists()) {
                    totalComErro++;
                    System.out.println("[ERRO] Arquivo nao encontrado: " + arquivo.getPath());
                    continue;
                }

                try {
                    carregarGrafo(arquivo);
                    System.out.println("\n--- " + subpasta.getName() + " | V=" + vertices + " M=" + arestas.size() + " ---");

                    if (algoritmo == 1) {
                        tarjan(detalharPontes);
                    } else {
                        int pontes = naive();
                        if (pontes == 0) {
                            System.out.println("Nenhuma ponte encontrada.");
                        } else if (detalharPontes) {
                            System.out.println("Total de pontes (naive): " + pontes);
                        } else {
                            System.out.println("Total de pontes: " + pontes);
                        }
                    }
                    totalProcessados++;
                } catch (FileNotFoundException e) {
                    totalComErro++;
                    System.out.println("[ERRO] arquivo nao encontrado - " + e.getMessage());
                } catch (Exception e) {
                    totalComErro++;
                    System.out.println("[ERRO] falha ao processar " + arquivo.getPath() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("\nResumo:");
        System.out.println("- Grafos processados: " + totalProcessados);
        System.out.println("- Erros: " + totalComErro);
    }
}