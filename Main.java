import java.io.*;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.*;
/*
    A maioria dos algoritmos utilizados foram reaproveitados das implementações anteriores pelo aluno Filipe Nery
    O algoritmo de Tarjan foi implementado utilizando como base o artigo do Tarjan, e também algumas modificações
    O algoritmo ingênuo foi implementado com base na definição clássica de ponte em grafos
    O algoritmo de Fleury foi inspirado no algoritmo ensinado em https://www.geeksforgeeks.org/dsa/fleurys-algorithm-for-printing-eulerian-path/

*/

class Vertice{
    // 
    public int vertice;
    public ArrayList<Integer> vizinhos;
    public boolean visitado;
    public int grau;

    Vertice(int num){
        this.vertice = num;
        this.vizinhos = new ArrayList<>();
        this.grau = 0;
        this.visitado = false;
    }

    public void inserir(int num){
        //Função que insere o vertice na lista de vizinhos
        this.vizinhos.add(num);
        this.grau++;
    }

    public int contarVizinhos(){
        return this.vizinhos.size();
    }

    public ArrayList<Integer> getVizinhos(){
        return this.vizinhos;
    }

    public void imprimir(){
        System.out.println("\nVeritice visualizado: " + vertice);
        System.out.println("(i)Grau de saída: " + contarVizinhos());
        System.out.print("(iii) Conjunto de vizinhos: ");
        for(int i: vizinhos){
            System.out.print(i + " ");
        }
        System.out.println("");
    }

    public void ordenar(){
        Collections.sort(vizinhos);
    }

    public void inserirOrdenado(int valor) {
        // Encontra a posição correta para manter a lista ordenada
        int pos = 0;
        while (pos < vizinhos.size() && vizinhos.get(pos) < valor) {
            pos++;
        }
        vizinhos.add(pos, valor);
        this.grau++;
    }

    public void remover(int valor) {
       boolean removeu = vizinhos.remove(Integer.valueOf(valor));
        if (removeu) {this.grau--; }
    }
}

class Grafos {
    static int vertice;
    static int arestas;
    static Vertice[] grafo;
    static int componentes;

    public static void lerGrafo(File arq) throws FileNotFoundException {
        //Leitura do arquivo de grafos
        
        try (Scanner leitor = new Scanner(arq)) {
           vertice = leitor.nextInt();
           arestas = leitor.nextInt();
           grafo = new Vertice[vertice + 1];
           for(int i = 1; i <= vertice; i++){
                grafo[i] = new Vertice(i);
            }

            for(int i = 0; i < arestas; i++){
                int pai = leitor.nextInt();
                int filho = leitor.nextInt();
                grafo[pai].inserir(filho);
                grafo[filho].inserir(pai);
            }
            for(int i = 1; i <= vertice; i++){
                grafo[i].ordenar();
            }
        }
        componentes = 0;
    }

    public static Vertice[] clonarGrafo() {
        Vertice[] novoGrafo = new Vertice[vertice + 1];
        for(int i = 1; i <= vertice; i++){
            novoGrafo[i] = new Vertice(i);
            for(int v : grafo[i].getVizinhos()) {
                novoGrafo[i].inserir(v);
            }
        }
        return novoGrafo;
    }

    public static int menu(){
        // Função responsavel por retornar qual aresta o usuario deseja visualizar as informações
        Scanner ler = new Scanner(System.in);
        System.out.printf("Qual vertice deseja visualizar(%d até %d): ", 1, vertice);
        int resp = ler.nextInt();
        return resp;
    }

    public static File solicitarArquivo() {
        Scanner ler = new Scanner(System.in);
        File arq = null;
        
        while(arq == null || !arq.exists()) {
            System.out.print("Insira o nome do arquivo a ser lido (não inserir o .txt): ");
            String nomeArq = ler.nextLine().trim() + ".txt";
            arq = new File(nomeArq);
            
            if(!arq.exists()) {
                System.out.println("\nErro: Arquivo '" + nomeArq + "' não encontrado!");
                
                // Lista arquivos .txt disponíveis no diretório
                File diretorioAtual = new File(".");
                File[] arquivosTxt = diretorioAtual.listFiles((dir, name) -> name.endsWith(".txt"));
                
                if(arquivosTxt != null && arquivosTxt.length > 0) {
                    System.out.println("Arquivos .txt disponíveis no diretório:");
                    for(File arquivo : arquivosTxt) {
                        String nome = arquivo.getName();
                        String nomeBase = nome.substring(0, nome.length() - 4); // Remove .txt
                        System.out.println("   • " + nomeBase);
                    }
                } else {
                    System.out.println("Nenhum arquivo .txt encontrado no diretório atual.");
                }
                
                System.out.print("\nDeseja tentar novamente? (s/n): ");
                String resposta = ler.nextLine().trim().toLowerCase();
                if(!resposta.equals("s") && !resposta.equals("sim")) {
                    System.out.println("Programa encerrado pelo usuário.");
                    System.exit(0);
                }
                System.out.println(); // Linha em branco para legibilidade
            }
        }
        return arq;
    }

    public static void removerAresta(int u, int v){
        // Grafo não direcionado: remove nos dois sentidos
        grafo[u].remover(v);
        grafo[v].remover(u);
    }

    public static void init(){
        try {
            File arq = solicitarArquivo();
            
            System.out.println("Carregando grafo...");
            lerGrafo(arq);    
            int visualizarVertice = menu();
            if(visualizarVertice >= 1 && visualizarVertice <= vertice) {
                grafo[visualizarVertice].imprimir();
            } else {
                System.out.println("Vértice inválido! Escolha entre 1 e " + vertice);
            }
        }
        catch(FileNotFoundException e){
            System.out.println("Erro: Arquivo não encontrado - " + e.getMessage());
        }
        catch(Exception e){
            System.out.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void dfsVerificaAlcanceDeVertice(int v, boolean[] visitado) {
        visitado[v] = true;

        for (int vizinho : Grafos.grafo[v].getVizinhos()) {
            if (!visitado[vizinho]) {
                dfsVerificaAlcanceDeVertice(vizinho, visitado);
            }
        }
    }
}

class Arestas{
    String aresta;
    String tipo;

    public Arestas(Vertice v, Vertice w, String tipo){
        this.aresta = v.vertice + ", " + w.vertice;
        this.tipo = tipo;
    }

    public void imprimir(){
        System.out.println("Aresta: " + aresta);
        System.out.println("Classificação: " + tipo);

    }
}

class DFS {
    static int t;
    static ArrayList<Arestas> arestas;
    static int td[];
    static int tt[];
    static int pai[];
    static int low[];

    private static ArrayList<Integer> vizinhosDe(Vertice v){
        return v.getVizinhos();
    }

    public static void imprimir(){
        for(Arestas i: arestas){
            i.imprimir();
            System.out.println("");
        }
    }
    public static void buscaProfundidade(Vertice v){
        t = t+1;
        td[v.vertice] = t;
        for(int ws: vizinhosDe(v))
        {
            Vertice w = Grafos.grafo[ws];
            if(w != null){
                if(td[w.vertice] == 0){
                    pai[w.vertice] = v.vertice;
                    arestas.add(new Arestas(v, w, "árvore"));
                    buscaProfundidade(w);
                }
                else{
                    if(tt[w.vertice] == 0){
                        arestas.add(new Arestas(v, w, "retorno"));
                    }
                    else if(td[v.vertice] < td[w.vertice]){
                        arestas.add(new Arestas(v, w, "avanço"));
                    }
                    else{
                        arestas.add(new Arestas(v, w, "cruzamento"));
                    }
                }
            }
        }
        t += 1;
        tt[v.vertice] = t;
    }

    public static void buscaProfundidadeTarjan(Vertice v, int p) {
        t++;
        td[v.vertice] = low[v.vertice] = t;
        // Controla arestas paralelas de volta ao pai
        int vezesVoltouAoPai = 0;
 
        for (int ws : v.getVizinhos()) {
            Vertice w = Grafos.grafo[ws];
            if (td[w.vertice] == 0) {
                // Vizinho não visitado: aresta de árvore
                pai[w.vertice] = v.vertice;
                arestas.add(new Arestas(v, w, "árvore"));
                buscaProfundidadeTarjan(w, v.vertice);
                // Propaga o menor low do filho para o pai
                low[v.vertice] = Math.min(low[v.vertice], low[w.vertice]);
            } else if (ws == p) {
                // É a aresta de volta ao pai — só ignora UMA vez (a da árvore)
                // Se aparecer de novo (aresta paralela), trata como retorno normal
                if (vezesVoltouAoPai == 0) {
                    vezesVoltouAoPai++;
                } else {
                    low[v.vertice] = Math.min(low[v.vertice], td[w.vertice]);
                }
            } else {
                // Vizinho já visitado que não é o pai: aresta de retorno
                low[v.vertice] = Math.min(low[v.vertice], td[w.vertice]);
                arestas.add(new Arestas(v, w, "retorno"));
            }
        }
    }


    public static void dfs() {
        t = 0;
        Grafos.componentes = 0;
        // Inicializar arrays
        int tam = Grafos.vertice + 1;
        td = new int[tam];
        tt = new int[tam];
        pai = new int[tam];
        arestas = new ArrayList<>();
        
        for (Vertice v : Grafos.grafo) {
            if(v != null){
                td[v.vertice] = 0;
                tt[v.vertice] = 0;
                pai[v.vertice] = 0;
            }
        }
        for(int i = 1; i <= Grafos.vertice; i++){
            if(td[i] == 0){
                buscaProfundidade(Grafos.grafo[i]);
                Grafos.componentes += 1;
            }
        }
    }

    public static void dfsComVerticeEspecifico(int op) {
        t = 0;
        Grafos.componentes = 0;
        // Inicializar arrays
        int tam = Grafos.vertice + 1;
        td = new int[tam];
        tt = new int[tam];
        pai = new int[tam];
        arestas = new ArrayList<>();
        
        for (Vertice v : Grafos.grafo) {
            if(v != null){
                td[v.vertice] = 0;
                tt[v.vertice] = 0;
                pai[v.vertice] = 0;
            }
        }
        for(int i = 1; i <= Grafos.vertice; i++){
            if(td[i] == 0){
                buscaProfundidade(Grafos.grafo[i]);
                Grafos.componentes += 1;
            }
        }
    }

    public static void teste() {
        try {
            File arq = Grafos.solicitarArquivo();
            Grafos.lerGrafo(arq);
            int op = Grafos.menu();
            dfsComVerticeEspecifico(op);
            imprimir();
        } catch (FileNotFoundException e) {
            System.out.println("Erro: Arquivo nao encontrado - " + e.getMessage());
        }
    }
}

class naive {
    // Classe auxiliar do algoritmo ingênuo
    public static ArrayList<String> pontes = new ArrayList<>();

    public static void cortar(int u, int v){
        Grafos.removerAresta(u, v);
    }
    

    public static void restaurar(int u, int v){
        //Função que restaura a aresta entre os vértices u e v  
        Grafos.grafo[u].inserirOrdenado(v);
        Grafos.grafo[v].inserirOrdenado(u);
    }

    public static void naive(){
        pontes.clear();
        DFS.dfs();
        int componentesOriginal = Grafos.componentes;
 
        for(int i = 1; i <= Grafos.vertice; i++){
            // Copia a lista para não iterar sobre a lista que será modificada
            ArrayList<Integer> vizinhos = new ArrayList<>(Grafos.grafo[i].getVizinhos());
            
            for(int j : vizinhos){
                // Evita checar a mesma aresta duas vezes (i-j e j-i)
                if(j <= i) continue;
 
                cortar(i, j);
                DFS.dfs();
 
                if(Grafos.componentes > componentesOriginal){
                    pontes.add(i + " - " + j);
                }
 
                restaurar(i, j);
            }      
        }
    }

    public void imprimirPontes() {
        if (pontes.isEmpty()) {
            System.out.println("Nenhuma ponte encontrada.");
        } else {
            for (String ponte : pontes) {
                System.out.println("Aresta " + ponte + " é uma ponte!");
            } naive.naive();
            System.out.println("Total: " + pontes.size() + " ponte(s)");
        }
    }

    // Verifica se uma aresta específica é ponte usando o algoritmo naive
    public static boolean isPonte(int u, int v) {
        DFS.dfs();
        int componentesOriginal = Grafos.componentes;
        
        cortar(u, v);
        DFS.dfs();
        boolean ehPonte = Grafos.componentes > componentesOriginal;
        restaurar(u, v);
        
        return ehPonte;
    }

}

class Tarjan {
    // Classe auxiliar do algoritmo de Tarjan
    public static ArrayList<String> pontes = new ArrayList<>();
 
    public static void tarjan() {
        pontes.clear();
        // Inicializar arrays zerados
        DFS.td = new int[Grafos.vertice + 1];
        DFS.low = new int[Grafos.vertice + 1];
        DFS.pai = new int[Grafos.vertice + 1];
        DFS.t = 0;
        DFS.arestas = new ArrayList<>();

        // Executar a busca em profundidade de Tarjan
        for (int i = 1; i <= Grafos.vertice; i++) {
            if (DFS.td[i] == 0) {
                DFS.buscaProfundidadeTarjan(Grafos.grafo[i], -1);
            }
        }

        // Verifica as pontes usando os resultados da DFS de Tarjan
        for (int i = 1; i <= Grafos.vertice; i++) {
            int p = DFS.pai[i];
            if (p != 0) { // Se o vértice 'i' tem um pai na árvore
                // A condição clássica: low[filho] > discovery[pai]
                if (DFS.low[i] > DFS.td[p]) {
                    pontes.add(p + " - " + i);
                }
            }
        }
    }

    public void imprimirPontes() {
        if (pontes.isEmpty()) {
            System.out.println("Nenhuma ponte encontrada.");
        } else {
            for (String ponte : pontes) {
                System.out.println("Aresta " + ponte + " é uma ponte!");
            }
            System.out.println("Total: " + pontes.size() + " ponte(s)");
        }
    }

    // Verifica se uma aresta específica é ponte usando o algoritmo de Tarjan
    public static boolean isPonte(int u, int v) {
        // Faz uma cópia do grafo para não interferir com a execução
        Vertice[] grafoOriginal = Grafos.grafo;
        Grafos.grafo = Grafos.clonarGrafo();
        
        // Executa Tarjan
        tarjan();
        
        // Verifica se a aresta (u,v) está nas pontes encontradas
        boolean ehPonte = false;
        for (String ponte : pontes) {
            String[] partes = ponte.split(" - ");
            int a = Integer.parseInt(partes[0]);
            int b = Integer.parseInt(partes[1]);
            if ((a == u && b == v) || (a == v && b == u)) {
                ehPonte = true;
                break;
            }
        }
        
        // Restaura o grafo original
        Grafos.grafo = grafoOriginal;
        return ehPonte;
    }
}

class CaminhoEuleriano {
    public static List<int[]> arestas;
    public static String algoritmoUsado = "naive"; // naive ou tarjan
    
    // Define qual algoritmo será usado para detectar pontes
    public static void setAlgoritmo(String algoritmo) {
        if (algoritmo.equalsIgnoreCase("naive") || algoritmo.equalsIgnoreCase("tarjan")) {
            algoritmoUsado = algoritmo.toLowerCase();
            System.out.println("Algoritmo de detecção de pontes: " + algoritmoUsado);
        } else {
            System.out.println("Algoritmo inválido! Use 'naive' ou 'tarjan'");
        }
    }
    
    static void contaVizinhos(int u, boolean[] visitado) {
        visitado[u] = true;
        for (int vizinho : Grafos.grafo[u].getVizinhos()) {
            if (!visitado[vizinho]) {
                contaVizinhos(vizinho, visitado);
            }
        }
    }

    // Verifica se a próxima aresta é válida (não é uma ponte)
    // usa o algoritmo definido em algoritmoUsado
    static boolean proximaArestaEValida(int u, int v) {

        if (Grafos.grafo[u].vizinhos.size() == 1) {
            return true;
        }

        boolean ehPonte;
        
        if (algoritmoUsado.equalsIgnoreCase("tarjan")) {
            ehPonte = Tarjan.isPonte(u, v);
        } else {
            ehPonte = naive.isPonte(u, v);
        }
        
        // Se for ponte, não é válida. Se não for, é válida.
        return !ehPonte;
    }

    static void pegarCaminho(int u, int v) {

        for (int i = 0; i < Grafos.grafo[u].vizinhos.size(); ++i) {
            int proximo = Grafos.grafo[u].vizinhos.get(i);
            if (proximaArestaEValida(u, proximo)) {
                arestas.add(new int[]{u, proximo});
                Grafos.removerAresta(u, proximo);
                pegarCaminho(proximo, v);
                break;
            }
        }
    }

    //retorna se o grafo é euleriano, semi, ou não
    public static String classificar() {
        boolean[] visitado = new boolean[Grafos.vertice + 1];
        int inicio = -1;
        for (int i = 1; i <= Grafos.vertice; i++) {
            if (Grafos.grafo[i].grau > 0) { inicio = i; break; }
        }
        if (inicio == -1) return "naoeuleriano";
        contaVizinhos(inicio, visitado);
        for (int i = 1; i <= Grafos.vertice; i++) {
            if (Grafos.grafo[i].grau > 0 && !visitado[i]) return "naoeuleriano";
        }
        int impares = 0;
        for (int i = 1; i <= Grafos.vertice; i++) {
            if (Grafos.grafo[i].grau % 2 != 0) impares++;
        }
        if (impares == 0) return "euleriano";
        if (impares == 2) return "semieuleriano";
        return "naoeuleriano";
    }
    
    static List<int[]> pegarCaminhoEuleriano(int v) {
        int inicio = 1;

        // Procura um vertice de grau ímpar para começar, se existir
        boolean temGrauImpar = false;
        int i = 1;
        while (i <= v && !temGrauImpar) {
            if (Grafos.grafo[i].grau % 2 != 0) {
                inicio = i;
                temGrauImpar = true;

            }
            i++;
        }

        arestas = new ArrayList<>();
        pegarCaminho(inicio, v);
        return arestas;
    }
    
}

public class Main {

    public static int menu() {
        System.out.println("Escolha o algoritmo para detecção de pontes:");
        System.out.println("1. Ingênuo");
        System.out.println("2. Tarjan");
        System.out.print("Opção: ");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt();
    }

    public static void main(String[] args) throws FileNotFoundException {
        // Inicia cronômetro
        long tempoInicio = System.nanoTime();
        
        Grafos.lerGrafo(new File("grafos100euleriano/grafo1/grafo.txt"));
        
        // Define qual algoritmo usar: "naive" ou "tarjan"
        int opcao = menu();
        if (opcao == 1) {
            CaminhoEuleriano.setAlgoritmo("naive");
        } else {
            CaminhoEuleriano.setAlgoritmo("tarjan");
        }

        List <int[]> caminhoEuleriano = CaminhoEuleriano.pegarCaminhoEuleriano(Grafos.vertice);
        for(int i = 0; i < caminhoEuleriano.size(); i++){
            System.out.print(caminhoEuleriano.get(i)[0] + "-" + caminhoEuleriano.get(i)[1]);
            if (i != caminhoEuleriano.size() - 1) {
                System.out.print(", ");
            }
        }
        
        // Calcula tempo de execução
        long tempoFim = System.nanoTime();
        long tempoExecucao = tempoFim - tempoInicio;
        
        System.out.println("\n\n=== ESTATÍSTICAS ===");
        System.out.println("Tempo de execução: " + (tempoExecucao / 1_000_000.0) + " ms");
    }
}

