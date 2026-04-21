import java.io.*;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.*;
/*
    Implementação do algoritmo de busca em profundidade (DFS) para grafos direcionados.
    O algoritmo foi adaptado dos slides da aula 07-Busca em profundidade.
    A classe Vertice e Grafos foram retirados da implementação anterior.
*/

class Vertice{
    // 
    public int vertice;
    public ArrayList<Integer> sucessao;
    public ArrayList<Integer> predecessao;
    public boolean visitado;

    Vertice(int num){
        this.vertice = num;
        this.sucessao = new ArrayList<>();
        this.predecessao = new ArrayList<>();
        this.visitado = false;
    }

    public void inserirS(int num){
        //Função que insere o vertice na lista de sucessores
        this.sucessao.add(num);
    }

    public void inserirP(int num){
        //Função que insere o vertice na lista de predecessores
        this.predecessao.add(num);
    }

    public int contarS(){
        return this.sucessao.size();
    }

    public int contarP(){
        return this.predecessao.size();
    }

    public ArrayList<Integer> getSucessores(){
        return this.sucessao;
    }

    public ArrayList<Integer> getPredecessores(){
        return this.predecessao;
    }

    public void imprimir(){
        System.out.println("\nVeritice visualizado: " + vertice);
        System.out.println("(i)Grau de saída: " + contarS());
        System.out.println("(ii)Grau de entrada: " + contarP());
        System.out.print("(iii) Conjunto de sucessores: ");
        for(int i : sucessao) {
            System.out.printf("%d, ", i);
        }
        System.out.print("\n(iv)Conjunto de predecessores: ");
        for(int i : predecessao) {
            System.out.printf("%d, ", i);
        }
        System.out.println("");
    }

    public void ordenar(){
        for(int i = 0; i < sucessao.size(); i++){
            for(int j = 0; j < sucessao.size() - 1; j++){
                if(sucessao.get(j) > sucessao.get(j+1)){
                    int temp = sucessao.get(j);
                    sucessao.set(j, sucessao.get(j+1));
                    sucessao.set(j+1, temp);
                }
            }
        }
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
                grafo[pai].inserirS(filho);
                grafo[filho].inserirP(pai);
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
            for(int s : grafo[i].getSucessores()) {
                novoGrafo[i].inserirS(s);
            }
            for(int p : grafo[i].getPredecessores()) {
                novoGrafo[i].inserirP(p);
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

public class DFS {
    static int t;
    static ArrayList<Arestas> arestas;
    static int td[];
    static int tt[];
    static int pai[];
    static int low[];

    private static ArrayList<Integer> sucessoresDe(Vertice v){
        return v.getSucessores();
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
        for(int ws: sucessoresDe(v))
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
        td[v.vertice] = low[v.vertice] = t; // Inicializa ambos com o tempo atual

        for (int ws : v.getSucessores()) {
            if (ws != p){
                Vertice w = Grafos.grafo[ws];
                if (td[w.vertice] == 0) { // Aresta de Árvore (não visitado)
                    pai[w.vertice] = v.vertice;
                    arestas.add(new Arestas(v, w, "árvore"));
                    
                    buscaProfundidadeTarjan(w, v.vertice);

                    // Na volta da recursão, o pai herda o menor tempo alcançado pelo filho
                    low[v.vertice] = Math.min(low[v.vertice], low[w.vertice]);
                } 
                else if (td[w.vertice] < td[v.vertice]) {
                    // Aresta de Retorno: atualiza low apenas para ancestrais
                    low[v.vertice] = Math.min(low[v.vertice], td[w.vertice]);
                    arestas.add(new Arestas(v, w, "retorno"));
                }
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