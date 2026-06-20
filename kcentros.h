#ifndef KCENTROS_H
#define KCENTROS_H

#include <stdbool.h>

// Valor seguro para representar o Infinito sem causar overflow nas somas
#define INF 4611686018427387903LL 

typedef struct {
    int n;          // Número de vértices
    long *dist;     // Matriz de distâncias achatada (1D)
} Grafo;

typedef struct {
    int *centros;
    long raio;
    char metodo[32];
} Resultado;

// Assinaturas de Funções
Grafo* carregar_pmed(const char* caminho, int *k_out);
void executar_floyd_warshall(Grafo *g);
Resultado aproximado(Grafo *g, int k);
Resultado exato_paralelo(Grafo *g, int k, Resultado limite_aprox);

#endif // KCENTROS_H