#include <stdio.h>
#include <stdlib.h>
#include "kcentros.h"

// Lê o formato "V E K" seguido de E linhas "u v peso"
Grafo* carregar_pmed(const char* caminho, int *k_out) {
    FILE *f = fopen(caminho, "r");
    if (!f) return NULL;
    
    int v, e, k;
    if (fscanf(f, "%d %d %d", &v, &e, &k) != 3) {
        fclose(f);
        return NULL;
    }
    *k_out = k;

    Grafo *g = malloc(sizeof(Grafo));
    g->n = v;
    g->dist = malloc(v * v * sizeof(long));

    for (int i = 0; i < v * v; i++) g->dist[i] = INF;
    for (int i = 0; i < v; i++) g->dist[i * v + i] = 0;

    for (int i = 0; i < e; i++) {
        int u, vv;
        long w;
        fscanf(f, "%d %d %ld", &u, &vv, &w);
        u--; vv--; // Conversão para índice 0
        g->dist[u * v + vv] = w;
        g->dist[vv * v + u] = w; // Grafo não direcionado
    }
    fclose(f);
    return g;
}

void executar_floyd_warshall(Grafo *g) {
    int n = g->n;
    long *dist = g->dist;
    
    for (int k = 0; k < n; k++) {
        int kn = k * n;
        for (int i = 0; i < n; i++) {
            int in = i * n;
            long dik = dist[in + k];
            
            if (dik < INF) {
                // Loop passível de vetorização (SIMD AVX) pelo compilador
                for (int j = 0; j < n; j++) {
                    long dkj = dist[kn + j];
                    long novaDist = dik + dkj;
                    if (dkj < INF && novaDist < dist[in + j]) {
                        dist[in + j] = novaDist;
                    }
                }
            }
        }
    }
}