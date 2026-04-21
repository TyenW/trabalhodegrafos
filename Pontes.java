import java.util.*;
import java.io.*;


public class Pontes {
    static boolean encontrouPonte;

    public static void cortar(int u, int v){
        //Função que corta a aresta entre os vértices u e v
        Grafos.grafo[u].sucessao.remove(Integer.valueOf(v));
        Grafos.grafo[v].predecessao.remove(Integer.valueOf(u));
    }

    public static void restaurar(int u, int v){
        //Função que restaura a aresta entre os vértices u e v    
        Grafos.grafo[u].inserirS(v);
        Grafos.grafo[v].inserirP(u);
    }

    public static void naive(){
        //Função que implementa o algoritmo ingênuo para encontrar pontes
        // Primeiro, contar componentes do grafo original
        DFS.dfs();
        int componentesOriginal = Grafos.componentes;
        for(int i = 1; i <= Grafos.vertice; i++){
            ArrayList<Integer> sucessores = new ArrayList<>(Grafos.grafo[i].getSucessores());
            
            for(int j : sucessores){
                // Cortar a aresta (i, j)
                cortar(i, j);
                
                // Executar DFS para contar componentes SEM essa aresta
                DFS.dfs();
                
                // Se aumentou componentes, é uma ponte
                if(Grafos.componentes > componentesOriginal){
                    System.out.println("Aresta " + i + " - " + j + " é uma ponte.");
                    encontrouPonte = true;
                }
                
                // Restaurar a aresta para a próxima iteração
                restaurar(i, j);
            }      
        }
    }

    public static void tarjan() {
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
        System.out.println("--- Relatório de Pontes ---");
        boolean encontrouPonte = false;
        for (int i = 1; i <= Grafos.vertice; i++) {
            int p = DFS.pai[i];
            if (p != 0) { // Se o vértice 'i' tem um pai na árvore
                // A condição clássica: low[filho] > discovery[pai]
                if (DFS.low[i] > DFS.td[p]) {
                    System.out.println("Aresta (" + p + ", " + i + ") é uma ponte!");
                    encontrouPonte = true;
                }
            }
        }
        if (!encontrouPonte) {
            System.out.println("Nenhuma ponte encontrada.");
        }
    }




    public static void main(String[] args) throws FileNotFoundException {
        Grafos.lerGrafo(new File("graph.txt"));
        System.out.println("Grafo lido com sucesso. Iniciando busca por pontes...");
        tarjan();
        System.out.println("Busca por pontes concluída.");
    }
}