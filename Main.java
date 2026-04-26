import java.io.*;
import java.util.*;

/*
 * ORIGENS E ATRIBUIÇÕES
 *
 * Estrutura base do grafo (Vertice, Grafos, leitura de arquivo):
 *   Reaproveitada das implementações anteriores pelo aluno Filipe Nery.
 *
 * Algoritmo Naïve de identificação de pontes:
 *   Implementado com base na definição clássica: remove cada aresta candidata
 *   e testa a conectividade via DFS. Complexidade O(E * (V+E)).
 *
 * Algoritmo de Tarjan (1974):
 *   Implementado com base no artigo original:
 *     Tarjan, R. E. (1974). A note on finding the bridges of a graph.
 *     Information Processing Letters, 2(6), 160-161.
 *     https://doi.org/10.1016/0020-0190(74)90003-9
 *   A lógica central — DFS com arrays td[] (tempo de descoberta) e low[]
 *   (menor td alcançável), com propagação do low ao pai na volta da recursão —
 *   é preservada intacta em relação ao artigo original.
 *
 *   OTIMIZAÇÃO DE IMPLEMENTAÇÃO — cache por passo no Fleury:
 *   Na versão direta, isPonte(u,v) executaria o Tarjan completo (O(V+E)) para
 *   cada aresta candidata dentro de um mesmo passo do Fleury. A otimização
 *   consiste em executar o Tarjan uma única vez por passo, armazenando as pontes
 *   encontradas em um HashSet<Long> (pares (u,v) codificados como long).
 *   O cache é invalidado após cada remoção de aresta; consultas dentro do mesmo
 *   passo respondem em O(1). A teoria do Tarjan não é alterada — apenas o momento
 *   da invocação é adiado até que seja necessário. Padrão análogo ao "lazy
 *   recomputation" descrito em Skiena, S. (2008). The Algorithm Design Manual
 *   (2nd ed.), Springer.
 *
 * Algoritmo de Fleury:
 *   Há um único método de Fleury, parametrizado pelo modo de detecção de pontes
 *   (Naive ou Tarjan). A lógica do Fleury — percorrer arestas não-ponte
 *   preferencialmente, recorrendo a pontes apenas quando não há alternativa —
 *   é fiel à descrição canônica em:
 *   https://www.geeksforgeeks.org/dsa/fleurys-algorithm-for-printing-eulerian-path/
 */

// ---------------------------------------------------------------------------
// Vertice
// ---------------------------------------------------------------------------
class Vertice {
    public int vertice;
    public ArrayList<Integer> vizinhos;
    public int grau;

    Vertice(int num) {
        this.vertice = num;
        this.vizinhos = new ArrayList<>();
        this.grau = 0;
    }

    public void inserir(int num) {
        this.vizinhos.add(num);
        this.grau++;
    }

    public int contarVizinhos() { return this.vizinhos.size(); }

    public ArrayList<Integer> getVizinhos() { return this.vizinhos; }

    public void ordenar() { Collections.sort(vizinhos); }

    public void inserirOrdenado(int valor) {
        int pos = 0;
        while (pos < vizinhos.size() && vizinhos.get(pos) < valor) pos++;
        vizinhos.add(pos, valor);
        this.grau++;
    }

    public void remover(int valor) {
        if (vizinhos.remove(Integer.valueOf(valor))) this.grau--;
    }
}

// ---------------------------------------------------------------------------
// Grafos
// ---------------------------------------------------------------------------
class Grafos {
    static int vertice;
    static int arestas;
    static Vertice[] grafo;
    static int componentes;

    public static void lerGrafo(File arq) throws FileNotFoundException {
        try (Scanner leitor = new Scanner(new BufferedReader(new FileReader(arq)))) {
            vertice = leitor.nextInt();
            arestas = leitor.nextInt();
            grafo   = new Vertice[vertice + 1];
            for (int i = 1; i <= vertice; i++) grafo[i] = new Vertice(i);
            for (int i = 0; i < arestas; i++) {
                int u = leitor.nextInt(), v = leitor.nextInt();
                grafo[u].inserir(v);
                grafo[v].inserir(u);
            }
            for (int i = 1; i <= vertice; i++) grafo[i].ordenar();
        }
        componentes = 0;
    }

    /** Copia profunda — necessaria para restaurar o grafo entre rodadas do batch. */
    public static Vertice[] clonarGrafo() {
        Vertice[] c = new Vertice[vertice + 1];
        for (int i = 1; i <= vertice; i++) {
            c[i] = new Vertice(i);
            for (int v : grafo[i].getVizinhos()) c[i].inserir(v);
        }
        return c;
    }

    public static void restaurarGrafo(Vertice[] backup) {
        for (int i = 1; i <= vertice; i++) {
            grafo[i].vizinhos = new ArrayList<>(backup[i].vizinhos);
            grafo[i].grau     = backup[i].grau;
        }
    }

    public static void removerAresta(int u, int v) {
        grafo[u].remover(v);
        grafo[v].remover(u);
    }

    public static void restaurarAresta(int u, int v) {
        grafo[u].inserirOrdenado(v);
        grafo[v].inserirOrdenado(u);
    }

    public static File solicitarArquivo(Scanner in) {
        File arq = null;
        while (arq == null || !arq.exists()) {
            System.out.print("Nome do arquivo (sem .txt): ");
            String nome = in.nextLine().trim() + ".txt";
            arq = new File(nome);
            if (!arq.exists()) {
                System.out.println("Arquivo '" + nome + "' nao encontrado.");
                File[] txts = new File(".").listFiles((d, n) -> n.endsWith(".txt"));
                if (txts != null && txts.length > 0) {
                    System.out.println("Disponiveis:");
                    for (File f : txts)
                        System.out.println("  " + f.getName().replace(".txt", ""));
                }
                System.out.print("Tentar novamente? (s/n): ");
                String resp = in.nextLine().trim().toLowerCase();
                if (!resp.equals("s") && !resp.equals("sim")) {
                    System.out.println("Programa encerrado.");
                    System.exit(0);
                }
            }
        }
        return arq;
    }
}

// ---------------------------------------------------------------------------
// DFS utilitaria
// ---------------------------------------------------------------------------
class DFS {
    static int   t;
    static int[] td;
    static int[] tt;
    static int[] pai;
    static int[] low;

    /** DFS completa para contagem de componentes conexas (usada pelo Naive). */
    public static void dfs() {
        t = 0;
        Grafos.componentes = 0;
        int tam = Grafos.vertice + 1;
        td = new int[tam]; tt = new int[tam]; pai = new int[tam];
        for (int i = 1; i <= Grafos.vertice; i++)
            if (td[i] == 0) { buscaProfundidade(i); Grafos.componentes++; }
    }

    private static void buscaProfundidade(int raiz) {
        Deque<int[]> pilha = new ArrayDeque<>();
        td[raiz] = ++t;
        pilha.push(new int[]{raiz, 0});
        while (!pilha.isEmpty()) {
            int[] frame = pilha.peek();
            int vId = frame[0];
            ArrayList<Integer> viz = Grafos.grafo[vId].getVizinhos();
            if (frame[1] < viz.size()) {
                int ws = viz.get(frame[1]++);
                if (td[ws] == 0) { pai[ws] = vId; td[ws] = ++t; pilha.push(new int[]{ws, 0}); }
            } else { tt[vId] = ++t; pilha.pop(); }
        }
    }

    /** DFS de alcancabilidade — usada na classificacao euleriana do Fleury. */
    public static void dfsAlcance(int raiz, boolean[] visitado) {
        Deque<Integer> pilha = new ArrayDeque<>();
        pilha.push(raiz);
        while (!pilha.isEmpty()) {
            int u = pilha.pop();
            if (visitado[u]) continue;
            visitado[u] = true;
            for (int viz : Grafos.grafo[u].getVizinhos())
                if (!visitado[viz]) pilha.push(viz);
        }
    }

    /**
     * DFS de Tarjan iterativa — calcula td[] e low[].
     * Fiel ao algoritmo original: low[v] = min(td[v], min(td[w]) para w
     * adjacente ja descoberto, min(low[filho]) para filhos na arvore DFS).
     * O frame guarda [vId, paiId, indiceVizinho, contadorVoltaAoPai].
     * O contador de volta ao pai trata a primeira aresta de retorno ao pai
     * como aresta de arvore (ignorada), e as demais como arestas de retorno
     * genuinas — comportamento correto para grafos simples sem arestas paralelas.
     */
    public static void buscaProfundidadeTarjan(int raiz, int paiRaiz) {
        Deque<int[]> pilha = new ArrayDeque<>();
        td[raiz] = low[raiz] = ++t;
        pilha.push(new int[]{raiz, paiRaiz, 0, 0});
        while (!pilha.isEmpty()) {
            int[] frame = pilha.peek();
            int vId = frame[0], pId = frame[1];
            ArrayList<Integer> viz = Grafos.grafo[vId].getVizinhos();
            if (frame[2] < viz.size()) {
                int ws = viz.get(frame[2]++);
                if (td[ws] == 0) {
                    pai[ws] = vId;
                    td[ws] = low[ws] = ++t;
                    pilha.push(new int[]{ws, vId, 0, 0});
                } else if (ws == pId) {
                    // Primeira volta ao pai: aresta de arvore — ignora
                    // Voltas subsequentes: aresta de retorno genuina
                    if (frame[3] == 0) frame[3]++;
                    else low[vId] = Math.min(low[vId], td[ws]);
                } else {
                    low[vId] = Math.min(low[vId], td[ws]);
                }
            } else {
                pilha.pop();
                if (!pilha.isEmpty())
                    low[pilha.peek()[0]] = Math.min(low[pilha.peek()[0]], low[vId]);
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Naive — identificacao de pontes por remocao e teste de conectividade
// ---------------------------------------------------------------------------
class Naive {

    /**
     * Verifica se (u,v) e ponte: remove a aresta, executa DFS para contar
     * componentes, restaura. Se o numero de componentes aumentou, e ponte.
     * Complexidade: O(V+E) por chamada — semantica exata do enunciado.
     */
    public static boolean isPonte(int u, int v) {
        DFS.dfs();
        int orig = Grafos.componentes;
        Grafos.removerAresta(u, v);
        DFS.dfs();
        boolean ehPonte = Grafos.componentes > orig;
        Grafos.restaurarAresta(u, v);
        return ehPonte;
    }
}

// ---------------------------------------------------------------------------
// Tarjan — identificacao de pontes via low[]
// ---------------------------------------------------------------------------
class Tarjan {

    /*
     * Cache de pontes para uso no Fleury.
     * O Tarjan e executado integralmente (O(V+E)) uma unica vez por passo;
     * o resultado e armazenado em um HashSet<Long> onde cada par (a,b), a<=b,
     * e codificado como ((long)a << 32) | b para acesso O(1).
     * O cache e invalidado (cacheValido = false) apos cada remocao de aresta,
     * forcando uma nova execucao do Tarjan no proximo passo que precisar de
     * informacao de ponte. A logica do Tarjan em si nao e alterada.
     */
    private static Set<Long> pontesCache = new HashSet<>();
    private static boolean   cacheValido = false;

    public static void invalidarCache() { cacheValido = false; }

    private static long chave(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /** Executa Tarjan completo e reconstroi o cache de pontes. */
    private static void reconstruirCache() {
        pontesCache.clear();
        int tam = Grafos.vertice + 1;
        DFS.td  = new int[tam];
        DFS.low = new int[tam];
        DFS.pai = new int[tam];
        DFS.t   = 0;
        for (int i = 1; i <= Grafos.vertice; i++)
            if (DFS.td[i] == 0) DFS.buscaProfundidadeTarjan(i, -1);
        for (int i = 1; i <= Grafos.vertice; i++) {
            int p = DFS.pai[i];
            if (p != 0 && DFS.low[i] > DFS.td[p])
                pontesCache.add(chave(p, i));
        }
        cacheValido = true;
    }

    /**
     * Consulta de ponte para uso no Fleury.
     * Reconstroi o cache (O(V+E)) apenas quando invalidado; caso contrario O(1).
     */
    public static boolean isPonte(int u, int v) {
        if (!cacheValido) reconstruirCache();
        return pontesCache.contains(chave(u, v));
    }
}

// ---------------------------------------------------------------------------
// CaminhoEuleriano — algoritmo de Fleury unico, parametrizado pelo modo
// ---------------------------------------------------------------------------
class CaminhoEuleriano {

    public enum Modo { NAIVE, TARJAN }

    public static List<int[]> arestas;

    /** Classifica o grafo: "euleriano", "semieuleriano" ou "naoeuleriano". */
    public static String classificar() {
        int inicio = -1;
        for (int i = 1; i <= Grafos.vertice; i++)
            if (Grafos.grafo[i].grau > 0) { inicio = i; break; }
        if (inicio == -1) return "naoeuleriano";

        boolean[] vis = new boolean[Grafos.vertice + 1];
        DFS.dfsAlcance(inicio, vis);
        for (int i = 1; i <= Grafos.vertice; i++)
            if (Grafos.grafo[i].grau > 0 && !vis[i]) return "naoeuleriano";

        int impares = 0;
        for (int i = 1; i <= Grafos.vertice; i++)
            if (Grafos.grafo[i].grau % 2 != 0) impares++;
        if (impares == 0) return "euleriano";
        if (impares == 2) return "semieuleriano";
        return "naoeuleriano";
    }

    /**
     * Verifica se a aresta (atual -> prox) e valida para o Fleury:
     * pode ser usada se nao for ponte, ou se for a unica aresta disponivel.
     */
    private static boolean arestaValida(int atual, int prox, Modo modo) {
        if (Grafos.grafo[atual].vizinhos.size() == 1) return true;
        return modo == Modo.NAIVE
                ? !Naive.isPonte(atual, prox)
                : !Tarjan.isPonte(atual, prox);
    }

    private static void percorrer(int inicio, Modo modo) {
        int atual = inicio;
        while (!Grafos.grafo[atual].vizinhos.isEmpty()) {
            // Snapshot para evitar ConcurrentModificationException
            int[] candidatos = Grafos.grafo[atual].vizinhos.stream()
                    .mapToInt(Integer::intValue).toArray();
            int escolhido = -1;
            for (int c : candidatos) {
                if (!Grafos.grafo[atual].vizinhos.contains(c)) continue;
                if (arestaValida(atual, c, modo)) { escolhido = c; break; }
            }
            if (escolhido == -1) break;
            arestas.add(new int[]{atual, escolhido});
            Grafos.removerAresta(atual, escolhido);
            if (modo == Modo.TARJAN) Tarjan.invalidarCache();
            atual = escolhido;
        }
    }

    /**
     * Ponto de entrada do Fleury.
     * Determina o vertice inicial (grau impar para semi-euleriano, qualquer
     * vertice ativo para euleriano) e executa o percurso.
     * Retorna lista vazia se o grafo nao admite caminho euleriano.
     */
    public static List<int[]> executar(Modo modo) {
        arestas = new ArrayList<>();
        if (classificar().equals("naoeuleriano")) return arestas;

        int inicio = -1;
        for (int i = 1; i <= Grafos.vertice; i++) {
            if (Grafos.grafo[i].grau > 0) {
                if (inicio == -1) inicio = i;
                if (Grafos.grafo[i].grau % 2 != 0) { inicio = i; break; }
            }
        }
        if (modo == Modo.TARJAN) Tarjan.invalidarCache();
        percorrer(inicio, modo);
        return arestas;
    }
}

// ---------------------------------------------------------------------------
// Experimento — execucao em batch com medicao de tempo e barra de progresso
// ---------------------------------------------------------------------------
class Experimento {

    private static final int LARGURA_BARRA = 40;

    /**
     * Atualiza a barra de progresso na mesma linha via \r.
     * Formato: [████░░░░░░░░░░░░░░░░░░░]  42%  |  1/5 rodadas  |  fleury-tarjan (2/5)
     */
    private static void imprimirBarra(int total, int feitos, int rodadaCompleta,
                                       int rodadas, String nomeAlg, int rodAlg) {
        int preenchido = total == 0 ? 0 : (feitos * LARGURA_BARRA) / total;
        int pct        = total == 0 ? 0 : (feitos * 100) / total;
        StringBuilder barra = new StringBuilder("[");
        for (int i = 0; i < preenchido;              i++) barra.append('\u2588');
        for (int i = preenchido; i < LARGURA_BARRA;  i++) barra.append('\u2591');
        barra.append("]");
        System.out.printf("\r%s %3d%%  |  %d/%d rodadas  |  %s (%d/%d)",
                barra, pct, rodadaCompleta, rodadas, nomeAlg, rodAlg, rodadas);
        System.out.flush();
    }

    /**
     * Executa o Fleury com os modos selecionados, 'rodadas' vezes cada.
     * O grafo e clonado antes e restaurado depois de cada rodada (Fleury
     * consome arestas). Os tempos sao gravados em tempos.txt ao final.
     *
     * @param modos    lista de modos a executar (NAIVE, TARJAN, ou ambos)
     * @param rodadas  numero de repeticoes por modo
     * @param arqGrafo arquivo fonte (para cabecalho do relatorio)
     */
    public static void rodar(List<CaminhoEuleriano.Modo> modos, int rodadas, File arqGrafo) {
        int totalExec  = modos.size() * rodadas;
        int execFeitas = 0;
        int rodadaCompleta = 0;

        String[] nomes = modos.stream()
                .map(m -> m == CaminhoEuleriano.Modo.NAIVE ? "fleury-naive" : "fleury-tarjan")
                .toArray(String[]::new);

        System.out.println("\nIniciando batch: " + rodadas + " rodada(s) x "
                + modos.size() + " algoritmo(s) = " + totalExec + " execucoes.\n");

        List<String> linhas = new ArrayList<>();
        linhas.add("Arquivo : " + arqGrafo.getName());
        linhas.add("Vertices: " + Grafos.vertice);
        linhas.add("Arestas : " + Grafos.arestas);
        linhas.add("Rodadas : " + rodadas);
        linhas.add("-------------------------------------------------------------------");
        linhas.add(String.format("%-16s  %6s  %14s  %14s",
                "algoritmo", "rodada", "tempo_ms", "tempo_s"));
        linhas.add("-------------------------------------------------------------------");

        imprimirBarra(totalExec, 0, 0, rodadas, nomes[0], 0);

        // acumula tempos para calcular media ao final
        double[][] temposMs = new double[modos.size()][rodadas];
        int[]      contagem = new int[modos.size()];

        for (int r = 1; r <= rodadas; r++) {
            for (int a = 0; a < modos.size(); a++) {
                imprimirBarra(totalExec, execFeitas, rodadaCompleta, rodadas, nomes[a], r);

                Vertice[] backup = Grafos.clonarGrafo();

                long ini = System.nanoTime();
                CaminhoEuleriano.executar(modos.get(a));
                long durNs = System.nanoTime() - ini;

                Grafos.restaurarGrafo(backup);
                execFeitas++;

                double durMs = durNs / 1_000_000.0;
                temposMs[a][contagem[a]++] = durMs;

                linhas.add(String.format("%-16s  %6d  %14.3f  %14.6f",
                        nomes[a], r, durMs, durNs / 1_000_000_000.0));
            }
            rodadaCompleta = r;
            imprimirBarra(totalExec, execFeitas, rodadaCompleta, rodadas,
                    rodadaCompleta < rodadas ? nomes[0] : "concluido", rodadaCompleta);
        }

        System.out.println();
        System.out.println();

        linhas.add("-------------------------------------------------------------------");
        for (int a = 0; a < modos.size(); a++) {
            double soma = 0;
            for (int k = 0; k < contagem[a]; k++) soma += temposMs[a][k];
            linhas.add(String.format("%-16s  %6s  %14.3f  %14.6f",
                    nomes[a] + " [MEDIA]", "-", soma / contagem[a],
                    soma / contagem[a] / 1000.0));
        }

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tempos.txt")))) {
            for (String l : linhas) pw.println(l);
            System.out.println("Resultados gravados em: "
                    + new File("tempos.txt").getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Erro ao gravar tempos.txt: " + e.getMessage());
        }
    }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
public class Main {

    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {

        // 1. Carrega o grafo
        File arq = Grafos.solicitarArquivo(in);
        try {
            Grafos.lerGrafo(arq);
        } catch (FileNotFoundException e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }
        System.out.println("Grafo carregado: " + Grafos.vertice
                + " vertices, " + Grafos.arestas + " arestas.");

        // 2. Modo de execucao
        System.out.print("Deseja realizar testes? (s/n): ");
        String resp = in.nextLine().trim().toLowerCase();

        if (resp.equals("s") || resp.equals("sim")) {
            modoBatch(arq);
        } else {
            modoIndividual();
        }
    }

    // -----------------------------------------------------------------------
    private static void modoBatch(File arqGrafo) {
        System.out.println("Algoritmo para os testes:");
        System.out.println("  1. Fleury-Naive");
        System.out.println("  2. Fleury-Tarjan");
        System.out.println("  3. Ambos");
        System.out.print("Opcao: ");
        int opcao = lerInt();

        List<CaminhoEuleriano.Modo> modos = new ArrayList<>();
        switch (opcao) {
            case 1: modos.add(CaminhoEuleriano.Modo.NAIVE);  break;
            case 2: modos.add(CaminhoEuleriano.Modo.TARJAN); break;
            case 3:
                modos.add(CaminhoEuleriano.Modo.NAIVE);
                modos.add(CaminhoEuleriano.Modo.TARJAN);
                break;
            default: System.out.println("Opcao invalida."); return;
        }

        System.out.print("Numero de rodadas: ");
        int rodadas = lerInt();
        if (rodadas < 1) { System.out.println("Numero invalido."); return; }

        Experimento.rodar(modos, rodadas, arqGrafo);
    }

    // -----------------------------------------------------------------------
    private static void modoIndividual() {
        System.out.println("Escolha o algoritmo:");
        System.out.println("  1. Fleury-Naive");
        System.out.println("  2. Fleury-Tarjan");
        System.out.print("Opcao: ");
        int opcao = lerInt();

        CaminhoEuleriano.Modo modo;
        String nomeAlg;
        switch (opcao) {
            case 1: modo = CaminhoEuleriano.Modo.NAIVE;  nomeAlg = "fleury-naive";  break;
            case 2: modo = CaminhoEuleriano.Modo.TARJAN; nomeAlg = "fleury-tarjan"; break;
            default: System.out.println("Opcao invalida."); return;
        }

        Vertice[] backup = Grafos.clonarGrafo();

        long ini = System.nanoTime();
        List<int[]> caminho = CaminhoEuleriano.executar(modo);
        long durNs = System.nanoTime() - ini;

        Grafos.restaurarGrafo(backup);

        if (caminho.isEmpty()) {
            System.out.println("O grafo nao possui caminho euleriano.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < caminho.size(); i++) {
                sb.append(caminho.get(i)[0]).append("-").append(caminho.get(i)[1]);
                if (i < caminho.size() - 1) sb.append(", ");
            }
            System.out.println(sb);
        }

        System.out.println("\n=== ESTATISTICAS ===");
        System.out.println("Vertices : " + Grafos.vertice);
        System.out.println("Arestas  : " + Grafos.arestas);
        System.out.println("Algoritmo: " + nomeAlg);
        System.out.printf( "Tempo    : %.3f ms%n",  durNs / 1_000_000.0);
        System.out.printf( "Tempo    : %.6f s%n",   durNs / 1_000_000_000.0);
    }

    private static int lerInt() {
        try { return Integer.parseInt(in.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}