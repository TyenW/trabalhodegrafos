import java.util.*;
import java.io.*;


class naive {
    // Classe auxiliar do algoritmo ingênuo
    public static ArrayList<String> pontes = new ArrayList<>();

    public static void cortar(int u, int v){
        // Grafo não direcionado: remove nos dois sentidos
        Grafos.grafo[u].remover(v);
        Grafos.grafo[v].remover(u);
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
            ArrayList<Integer> vizinhos = new ArrayList<>(Grafos.grafo[i].getSucessores());
            
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
}

public class Pontes {
    
    public static void main(String[] args) throws FileNotFoundException {
        // Carregar o grafo a partir do arquivo
        Grafos.lerGrafo(new File("graph.txt"));

        // Executar o algoritmo de Tarjan
        System.out.println("Executando algoritmo de Tarjan...");
        Tarjan t = new Tarjan();
        System.out.println("\n--- Relatório de Pontes (Tarjan) ---");   
        t.tarjan();
        t.imprimirPontes();
        
        // Executar o algoritmo ingênuo
        System.out.println("\nExecutando algoritmo ingênuo...");
        naive n = new naive();
        n.naive();
        
        System.out.println("\n--- Relatório de Pontes (Naive) ---");
        n.imprimirPontes();
        
    }
}