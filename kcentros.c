#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <omp.h>
#include "kcentros.h"

// Variável Global Atómica para Partilha do Teto de Branch & Bound
long melhor_raio_global = INF;

// Método interno de cálculo e poda Fail-Fast
static long calcular_raio_com_poda(Grafo *g, int *centros, int k, long limite_teto) {
    int n = g->n;
    long raio = -1;
    long *dist = g->dist;

    for (int i = 0; i < n; i++) {
        long min_linha = INF;
        int in = i * n;
        
        for (int c = 0; c < k; c++) {
            long d = dist[in + centros[c]];
            if (d < min_linha) min_linha = d;
        }
        
        if (min_linha > raio) raio = min_linha;
        if (raio >= limite_teto) return INF; // Poda Branch & Bound ativada
    }
    return raio;
}

// 1. Método Greedy (Aproximado)
Resultado aproximado(Grafo *g, int k) {
    int n = g->n;
    long *minDist = malloc(n * sizeof(long));
    for (int i = 0; i < n; i++) minDist[i] = INF;

    Resultado res;
    res.centros = malloc(k * sizeof(int));
    strcpy(res.metodo, "APROXIMADO");

    res.centros[0] = 0;
    for (int i = 0; i < n; i++) {
        long d = g->dist[i * n + 0];
        if (d < minDist[i]) minDist[i] = d;
    }

    for (int c = 1; c < k; c++) {
        int proximo = -1;
        long maxDist = -1;
        for (int i = 0; i < n; i++) {
            if (minDist[i] > maxDist) {
                maxDist = minDist[i];
                proximo = i;
            }
        }
        res.centros[c] = proximo;
        for (int i = 0; i < n; i++) {
            long d = g->dist[i * n + proximo];
            if (d < minDist[i]) minDist[i] = d;
        }
    }
    
    res.raio = calcular_raio_com_poda(g, res.centros, k, INF);
    free(minDist);
    return res;
}

// Lógica de progressão das combinações iterativas
static bool proxima_combinacao_restrita(int *comb, int n, int k) {
    int i = k - 1;
    // Impede alterar comb[0] (usado para dividir as tarefas entre threads)
    while (i > 0 && comb[i] == n - k + i) i--;
    if (i == 0) return false;
    
    comb[i]++;
    for (int j = i + 1; j < k; j++) comb[j] = comb[j - 1] + 1;
    return true;
}

// 2. Método Exato com Paralelização OpenMP
Resultado exato_paralelo(Grafo *g, int k, Resultado limite_aprox) {
    int n = g->n;
    melhor_raio_global = limite_aprox.raio;

    Resultado res;
    res.centros = malloc(k * sizeof(int));
    memcpy(res.centros, limite_aprox.centros, k * sizeof(int));
    strcpy(res.metodo, "EXATO_PARALELO");

    // Início da Região Paralela OpenMP
    #pragma omp parallel
    {
        int *melhor_local_centros = malloc(k * sizeof(int));
        long melhor_local_raio = melhor_raio_global;

        // Distribui os ramos da árvore por todos os núcleos disponíveis
        #pragma omp for schedule(dynamic) nowait
        for (int s = 0; s <= n - k; s++) {
            int *comb = malloc(k * sizeof(int));
            comb[0] = s;
            for (int i = 1; i < k; i++) comb[i] = s + i;

            do {
                long teto_atual;
                // Lê o raio mais baixo encontrado por qualquer thread de forma segura
                #pragma omp atomic read
                teto_atual = melhor_raio_global;

                long raio = calcular_raio_com_poda(g, comb, k, teto_atual);

                if (raio < teto_atual) {
                    melhor_local_raio = raio;
                    memcpy(melhor_local_centros, comb, k * sizeof(int));

                    // Atualiza a barreira atómica apenas se continuarmos a ter o melhor raio
                    #pragma omp critical
                    {
                        if (raio < melhor_raio_global) {
                            melhor_raio_global = raio;
                            memcpy(res.centros, comb, k * sizeof(int));
                        }
                    }
                }
            } while (proxima_combinacao_restrita(comb, n, k));
            free(comb);
        }
        free(melhor_local_centros);
    }
    
    res.raio = melhor_raio_global;
    return res;
}