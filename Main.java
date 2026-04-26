import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/*
 * ORIGENS E ATRIBUIÇÕES
 *
 * Estrutura base do grafo (Vertice, Grafos, leitura de arquivo):
 *   Reaproveitada das implementações anteriores pelo aluno Filipe Nery.
 *
 * Algoritmo Naïve de identificação de pontes:
 *   Implementado com base na definição clássica: remove cada aresta e testa
 *   conectividade via DFS. Complexidade O(E * (V+E)).
 *
 * Algoritmo de Tarjan (1974):
 *   Implementado com base no artigo original:
 *     Tarjan, R. E. (1974). A note on finding the bridges of a graph.
 *     Information Processing Letters, 2(6), 160-161.
 *     https://doi.org/10.1016/0020-0190(74)90003-9
 *   A lógica central (low[], td[], propagação do low ao pai) é fiel ao artigo.
 *
 *   VARIANTE 1 — Tarjan com cache por passo (isPonte):
 *   Executa o Tarjan completo (O(V+E)) uma única vez por passo do Fleury,
 *   armazenando o resultado em um HashSet de pares codificados como long.
 *   O cache é invalidado após cada remoção de aresta; consultas dentro do
 *   mesmo passo respondem em O(1). Complexidade total do Fleury: O(E*(V+E)).
 *   A ideia de recomputação preguiçosa é análoga ao padrão "lazy recomputation"
 *   descrito em Skiena, S. (2008). The Algorithm Design Manual (2nd ed.), Springer.
 *
 *   VARIANTE 2 — Tarjan otimizado com DFS de alcançabilidade (isPonteOtimizado):
 *   Para verificar se (u,v) é ponte no Fleury, substitui o Tarjan global por
 *   uma DFS de alcançabilidade local: remove (u,v), verifica se v ainda é
 *   alcançável a partir de u, restaura. O(V+E) por candidata, sem recomputar
 *   arrays globais. Mantém o critério de ponte (desconexão do grafo) conceitualmente
 *   fiel ao enunciado. A lógica do Tarjan (low[], td[]) é preservada intacta;
 *   apenas o mecanismo de consulta no Fleury é substituído por DFS de alcance.
 *
 * Algoritmo de Fleury:
 *   Inspirado na descrição canônica disponível em:
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

    public void imprimir() {
        System.out.println("\nVertice visualizado: " + vertice);
        System.out.println("(i) Grau: " + contarVizinhos());
        System.out.print("(ii) Conjunto de vizinhos: ");
        for (int i : vizinhos) System.out.print(i + " ");
        System.out.println();
    }

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

    /** Cópia profunda do grafo — necessária para restaurar entre rodadas do batch. */
    public static Vertice[] clonarGrafo() {
        Vertice[] c = new Vertice[vertice + 1];
        for (int i = 1; i <= vertice; i++) {
            c[i] = new Vertice(i);
            for (int v : grafo[i].getVizinhos()) c[i].inserir(v);
        }
        return c;
    }

    /** Restaura o grafo principal a partir de um backup obtido via clonarGrafo(). */
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
                System.out.println("Arquivo '" + nome + "' não encontrado.");
                File[] txts = new File(".").listFiles((d, n) -> n.endsWith(".txt"));
                if (txts != null && txts.length > 0) {
                    System.out.println("Disponíveis:");
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
// DFS utilitária
// ---------------------------------------------------------------------------
class DFS {
    static int   t;
    static int[] td;
    static int[] tt;
    static int[] pai;
    static int[] low;

    /** DFS completa para contagem de componentes conexas. */
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

    /**
     * DFS de alcançabilidade a partir de 'raiz'.
     * Preenche visitado[] com todos os vértices alcançáveis no grafo atual.
     */
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
     * DFS de Tarjan iterativa — calcula low[] e td[].
     * frame: [vId, paiId, índiceVizinho, contadorRetornoAoPai]
     * O contadorRetornoAoPai trata arestas paralelas: a primeira volta ao pai
     * é ignorada (aresta de árvore); as demais atualizam low normalmente.
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
                    pai[ws] = vId; td[ws] = low[ws] = ++t;
                    pilha.push(new int[]{ws, vId, 0, 0});
                } else if (ws == pId) {
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
// Naive — identificação de pontes por remoção e teste de conectividade
// ---------------------------------------------------------------------------
class Naive {
    public static ArrayList<String> pontes = new ArrayList<>();

    /** Lista todas as pontes do grafo via força bruta. */
    public static void naive() {
        pontes.clear();
        DFS.dfs();
        int orig = Grafos.componentes;
        for (int i = 1; i <= Grafos.vertice; i++) {
            ArrayList<Integer> viz = new ArrayList<>(Grafos.grafo[i].getVizinhos());
            for (int j : viz) {
                if (j <= i) continue;
                Grafos.removerAresta(i, j);
                DFS.dfs();
                if (Grafos.componentes > orig) pontes.add(i + " - " + j);
                Grafos.restaurarAresta(i, j);
            }
        }
    }

    /**
     * Verifica se (u,v) é ponte: remove, testa conectividade via DFS, restaura.
     * Semântica do enunciado preservada. Complexidade: O(V+E) por chamada.
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
// Tarjan — identificação de pontes via low[]
// ---------------------------------------------------------------------------
class Tarjan {
    public static ArrayList<String> pontes = new ArrayList<>();

    // Cache de pontes — Variante 1 (cache por passo no Fleury)
    private static Set<Long> pontesCache = new HashSet<>();
    private static boolean   cacheValido = false;

    public static void invalidarCache() { cacheValido = false; }

    private static long chave(int a, int b) {
        if (a > b) { int t = a; a = b; b = t; }
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }

    /** Executa Tarjan completo e popula Tarjan.pontes. */
    public static void tarjan() {
        pontes.clear();
        int tam = Grafos.vertice + 1;
        DFS.td = new int[tam]; DFS.low = new int[tam];
        DFS.pai = new int[tam]; DFS.t = 0;
        for (int i = 1; i <= Grafos.vertice; i++)
            if (DFS.td[i] == 0) DFS.buscaProfundidadeTarjan(i, -1);
        for (int i = 1; i <= Grafos.vertice; i++) {
            int p = DFS.pai[i];
            if (p != 0 && DFS.low[i] > DFS.td[p]) pontes.add(p + " - " + i);
        }
    }

    private static void reconstruirCache() {
        pontesCache.clear();
        int tam = Grafos.vertice + 1;
        DFS.td = new int[tam]; DFS.low = new int[tam];
        DFS.pai = new int[tam]; DFS.t = 0;
        for (int i = 1; i <= Grafos.vertice; i++)
            if (DFS.td[i] == 0) DFS.buscaProfundidadeTarjan(i, -1);
        for (int i = 1; i <= Grafos.vertice; i++) {
            int p = DFS.pai[i];
            if (p != 0 && DFS.low[i] > DFS.td[p]) pontesCache.add(chave(p, i));
        }
        cacheValido = true;
    }

    /**
     * Variante 1 — cache por passo:
     * Tarjan reexecutado apenas quando o cache está inválido (após remoção de aresta).
     * Custo: O(V+E) uma vez por passo do Fleury; consultas subsequentes em O(1).
     */
    public static boolean isPonte(int u, int v) {
        if (!cacheValido) reconstruirCache();
        return pontesCache.contains(chave(u, v));
    }

    /**
     * Variante 2 — DFS de alcançabilidade local:
     * Remove (u,v), verifica alcançabilidade de v a partir de u, restaura.
     * Equivalente ao critério low[v] > td[u] do Tarjan para grafos simples.
     * Evita recomputar arrays globais; O(V+E) por candidata.
     */
    public static boolean isPonteOtimizado(int u, int v) {
        if (Grafos.grafo[u].vizinhos.size() == 1) return true;
        Grafos.removerAresta(u, v);
        boolean[] visitado = new boolean[Grafos.vertice + 1];
        DFS.dfsAlcance(u, visitado);
        boolean ehPonte = !visitado[v];
        Grafos.restaurarAresta(u, v);
        return ehPonte;
    }

    public void imprimirPontes() {
        if (pontes.isEmpty()) {
            System.out.println("Nenhuma ponte encontrada.");
        } else {
            for (String p : pontes) System.out.println("Aresta " + p + " e uma ponte.");
            System.out.println("Total: " + pontes.size() + " ponte(s)");
        }
    }
}

// ---------------------------------------------------------------------------
// CaminhoEuleriano — algoritmo de Fleury (três variantes de isPonte)
// ---------------------------------------------------------------------------
class CaminhoEuleriano {

    public enum Modo { NAIVE, TARJAN_ORIGINAL, TARJAN_OTIMIZADO }

    public static List<int[]> arestas;

    /**
     * Flag de cancelamento — setada pela thread de timeout do Experimento.
     * O loop do percorrer verifica esta flag a cada passo e para imediatamente
     * se ela estiver ativa, permitindo que o timeout seja efetivo.
     */
    static volatile boolean cancelado = false;

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

    private static boolean arestaValida(int atual, int prox, Modo modo) {
        if (Grafos.grafo[atual].vizinhos.size() == 1) return true;
        switch (modo) {
            case NAIVE:            return !Naive.isPonte(atual, prox);
            case TARJAN_ORIGINAL:  return !Tarjan.isPonte(atual, prox);
            case TARJAN_OTIMIZADO: return !Tarjan.isPonteOtimizado(atual, prox);
            default: return false;
        }
    }

    private static void percorrer(int inicio, Modo modo) {
        int atual = inicio;
        // Verifica cancelado a cada passo — permite interrupção por timeout
        while (!cancelado && !Grafos.grafo[atual].vizinhos.isEmpty()) {
            int[] candidatos = Grafos.grafo[atual].vizinhos.stream()
                    .mapToInt(Integer::intValue).toArray();
            int escolhido = -1;
            for (int c : candidatos) {
                if (cancelado) return;
                if (!Grafos.grafo[atual].vizinhos.contains(c)) continue;
                if (arestaValida(atual, c, modo)) { escolhido = c; break; }
            }
            if (escolhido == -1) break;
            arestas.add(new int[]{atual, escolhido});
            Grafos.removerAresta(atual, escolhido);
            if (modo == Modo.TARJAN_ORIGINAL) Tarjan.invalidarCache();
            atual = escolhido;
        }
    }

    /**
     * Executa Fleury com o modo especificado.
     * Retorna lista vazia se o grafo não admite caminho euleriano ou foi cancelado.
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
        if (modo == Modo.TARJAN_ORIGINAL) Tarjan.invalidarCache();
        percorrer(inicio, modo);
        return arestas;
    }
}

// ---------------------------------------------------------------------------
// Experimento — execução em batch com medição de tempo e barra de progresso
// ---------------------------------------------------------------------------
class Experimento {

    private static final long LIMITE_NS = 4L * 1_000_000_000L;

    private static final String[] NOMES = {
        "naive-fleury",
        "tarjan-fleury-original",
        "tarjan-fleury-otimizado"
    };

    private static final CaminhoEuleriano.Modo[] MODOS = {
        CaminhoEuleriano.Modo.NAIVE,
        CaminhoEuleriano.Modo.TARJAN_ORIGINAL,
        CaminhoEuleriano.Modo.TARJAN_OTIMIZADO
    };

    private static final int LARGURA_BARRA = 40;

    /**
     * Imprime (ou atualiza no lugar) a barra de progresso.
     * Usa \r para sobrescrever a linha atual — funciona em terminais ANSI.
     *
     * Formato:
     *   [████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░]  42%  |  1/5 rodadas  |  naive-fleury (2/3)
     *
     * execTotal  : total de execuções individuais (MODOS.length * rodadas)
     * execFeitas : quantas já concluíram
     * rodadaAtual: índice da rodada global completa (todas 3 feitas) — para "X/N rodadas"
     * rodadas    : total de rodadas pedidas
     * nomeAlg    : algoritmo em execução agora
     * rodAlg     : rodada atual deste algoritmo
     * totalAlg   : total de rodadas deste algoritmo
     */
    private static void imprimirBarra(int execTotal, int execFeitas,
                                      int rodadaCompleta, int rodadas,
                                      String nomeAlg, int rodAlg, int totalAlg) {
        int preenchido = (execTotal == 0) ? 0 : (execFeitas * LARGURA_BARRA) / execTotal;
        int vazio      = LARGURA_BARRA - preenchido;
        int pct        = (execTotal == 0) ? 0 : (execFeitas * 100) / execTotal;

        StringBuilder barra = new StringBuilder("[");
        for (int i = 0; i < preenchido; i++) barra.append('\u2588'); // bloco cheio
        for (int i = 0; i < vazio;      i++) barra.append('\u2591'); // bloco vazio
        barra.append("]");

        String linha = String.format("\r%s %3d%%  |  %d/%d rodadas  |  %s (%d/%d)",
                barra, pct, rodadaCompleta, rodadas, nomeAlg, rodAlg, totalAlg);

        System.out.print(linha);
        System.out.flush();
    }

    /**
     * Roda cada algoritmo 'rodadas' vezes com timeout real de 60s por execução.
     *
     * Mecanismo de timeout:
     *   O Fleury é submetido a um ExecutorService de thread única.
     *   Se não concluir em 60s, o Future é cancelado e a flag
     *   CaminhoEuleriano.cancelado é setada — o loop do percorrer para
     *   na próxima verificação da flag. O grafo é restaurado antes da
     *   próxima rodada independentemente do resultado.
     *
     * Exibe barra de progresso em tempo real.
     * Resultados gravados em tempos.txt.
     */
    public static void rodar(int rodadas, File arqGrafo) {
        int totalExec      = MODOS.length * rodadas;
        int execFeitas     = 0;
        int rodadaCompleta = 0;

        System.out.println("Iniciando batch: " + rodadas
                + " rodada(s) x " + MODOS.length + " algoritmos"
                + " = " + totalExec + " execucoes no total.");
        System.out.println();

        List<String> linhas = new ArrayList<>();
        linhas.add("Arquivo : " + arqGrafo.getName());
        linhas.add("Vertices: " + Grafos.vertice);
        linhas.add("Arestas : " + Grafos.arestas);
        linhas.add("Rodadas : " + rodadas);
        linhas.add("Limite  : 60 s por execucao");
        linhas.add("-------------------------------------------------------------------");
        linhas.add(String.format("%-28s  %6s  %14s  %14s  %s",
                "algoritmo", "rodada", "tempo_ms", "tempo_s", "status"));
        linhas.add("-------------------------------------------------------------------");

        imprimirBarra(totalExec, 0, 0, rodadas, NOMES[0], 0, rodadas);

        double[][] temposMs = new double[MODOS.length][rodadas];
        int[]      contagem = new int[MODOS.length];
        boolean[]  inviavel = new boolean[MODOS.length];

        // Thread única para rodar o Fleury — permite interrupção via Future.cancel()
        ExecutorService exec = Executors.newSingleThreadExecutor();

        for (int r = 1; r <= rodadas; r++) {
            for (int a = 0; a < MODOS.length; a++) {
                final int algIdx = a;

                if (inviavel[a]) {
                    execFeitas++;
                    imprimirBarra(totalExec, execFeitas,
                            execFeitas / MODOS.length, rodadas,
                            NOMES[a] + " [PULADO]", r, rodadas);
                    continue;
                }

                imprimirBarra(totalExec, execFeitas, rodadaCompleta, rodadas,
                        NOMES[a], r, rodadas);

                Vertice[] backup = Grafos.clonarGrafo();

                // Reseta flag antes de cada execução
                CaminhoEuleriano.cancelado = false;

                final CaminhoEuleriano.Modo modo = MODOS[a];
                long ini = System.nanoTime();

                Future<?> future = exec.submit(() -> CaminhoEuleriano.executar(modo));

                boolean timedOut = false;
                try {
                    future.get(60, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    // Para o loop do Fleury na próxima verificação da flag
                    CaminhoEuleriano.cancelado = true;
                    future.cancel(true);
                    // Aguarda a thread realmente parar antes de restaurar o grafo
                    try { future.get(5, TimeUnit.SECONDS); } catch (Exception ignored) {}
                    timedOut = true;
                } catch (InterruptedException | ExecutionException e) {
                    CaminhoEuleriano.cancelado = true;
                    future.cancel(true);
                    timedOut = true;
                }

                long durNs = System.nanoTime() - ini;

                // Restaura o grafo sempre — independente de timeout ou sucesso
                Grafos.restaurarGrafo(backup);
                execFeitas++;

                double durMs = durNs / 1_000_000.0;
                double durS  = durNs / 1_000_000_000.0;
                String status;

                if (timedOut) {
                    status = "TIMEOUT — inviavel para este grafo";
                    inviavel[algIdx] = true;
                } else {
                    status = "ok";
                    temposMs[algIdx][contagem[algIdx]] = durMs;
                    contagem[algIdx]++;
                }

                linhas.add(String.format("%-28s  %6d  %14.3f  %14.6f  %s",
                        NOMES[a], r, durMs, durS, status));

                if (timedOut && r < rodadas) {
                    linhas.add(String.format("%-28s  %6s  %14s  %14s  %s",
                            NOMES[a], (r + 1) + ".." + rodadas, "-", "-",
                            "PULADO (timeout na rodada " + r + ")"));
                }
            }

            rodadaCompleta = r;
            imprimirBarra(totalExec, execFeitas, rodadaCompleta, rodadas,
                    rodadaCompleta < rodadas ? NOMES[0] : "concluido",
                    rodadaCompleta, rodadas);
        }

        exec.shutdownNow();

        System.out.println();
        System.out.println();

        linhas.add("-------------------------------------------------------------------");
        for (int a = 0; a < MODOS.length; a++) {
            linhas.add("");
            if (contagem[a] > 0) {
                double soma = 0;
                for (int k = 0; k < contagem[a]; k++) soma += temposMs[a][k];
                double media = soma / contagem[a];
                linhas.add(String.format("%-28s  %6s  %14.3f  %14.6f  %s",
                        NOMES[a] + " [MEDIA]", "-", media, media / 1000.0, "-"));
            } else {
                linhas.add(String.format("%-28s  %6s  %14s  %14s  %s",
                        NOMES[a] + " [MEDIA]", "-", "-", "-",
                        "sem dados (todas as rodadas excederam o limite)"));
            }
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

        // 1. Solicita e carrega o arquivo
        File arq = Grafos.solicitarArquivo(in);
        try {
            Grafos.lerGrafo(arq);
        } catch (FileNotFoundException e) {
            System.out.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }
        System.out.println("Grafo carregado: " + Grafos.vertice
                + " vertices, " + Grafos.arestas + " arestas.");

        // 2. Modo de execução
        System.out.print("Executar teste em batch (todos os algoritmos N vezes)? (s/n): ");
        String resp = in.nextLine().trim().toLowerCase();

        if (resp.equals("s") || resp.equals("sim")) {
            modoBatch(arq);
        } else {
            modoIndividual();
        }
    }

    // -----------------------------------------------------------------------
    private static void modoBatch(File arqGrafo) {
        System.out.print("Numero de rodadas por algoritmo: ");
        int rodadas = lerInt();
        if (rodadas < 1) { System.out.println("Numero invalido."); return; }
        Experimento.rodar(rodadas, arqGrafo);
    }

    // -----------------------------------------------------------------------
    private static void modoIndividual() {
        System.out.println("Escolha o algoritmo:");
        System.out.println("  1. Naive");
        System.out.println("  2. Tarjan original (cache por passo)");
        System.out.println("  3. Tarjan otimizado (DFS de alcancabilidade)");
        System.out.print("Opcao: ");
        int opcao = lerInt();

        CaminhoEuleriano.Modo modo;
        String nomeAlg;
        switch (opcao) {
            case 1: modo = CaminhoEuleriano.Modo.NAIVE;
                    nomeAlg = "naive"; break;
            case 2: modo = CaminhoEuleriano.Modo.TARJAN_ORIGINAL;
                    nomeAlg = "tarjan-original"; break;
            case 3: modo = CaminhoEuleriano.Modo.TARJAN_OTIMIZADO;
                    nomeAlg = "tarjan-otimizado"; break;
            default: System.out.println("Opcao invalida."); return;
        }

        Vertice[] backup = Grafos.clonarGrafo();

        long ini = System.nanoTime();
        List<int[]> caminho = CaminhoEuleriano.executar(modo);
        long fim = System.nanoTime();
        long durNs = fim - ini;

        Grafos.restaurarGrafo(backup);

        // Imprime caminho no terminal
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

        // Estatísticas no terminal
        System.out.println("\n=== ESTATISTICAS ===");
        System.out.println("Vertices : " + Grafos.vertice);
        System.out.println("Arestas  : " + Grafos.arestas);
        System.out.println("Algoritmo: " + nomeAlg);
        System.out.printf( "Tempo    : %.3f ms%n",  durNs / 1_000_000.0);
        System.out.printf( "Tempo    : %.6f s%n",   durNs / 1_000_000_000.0);
        if (durNs > 4L * 1_000_000_000L)
            System.out.println("AVISO: tempo excedeu 60 s — algoritmo inviavel para este grafo.");
    }

    private static int lerInt() {
        try { return Integer.parseInt(in.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}