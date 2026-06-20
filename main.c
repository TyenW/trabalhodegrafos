#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <omp.h>
#include "kcentros.h"

long long tempo_ms() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (((long long)tv.tv_sec) * 1000) + (tv.tv_usec / 1000);
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        printf("Uso: %s <arquivo_pmed.txt>\n", argv[0]);
        return 1;
    }

    int k = 0;
    printf("--- Solucionador Nativo (C/OpenMP) de K-Centros ---\n");
    printf("Processadores alocados: %d\n", omp_get_max_threads());
    printf("Carregando instância: %s...\n", argv[1]);
    
    Grafo *g = carregar_pmed(argv[1], &k);
    if (!g) {
        printf("Falha ao abrir ou ler o formato do arquivo.\n");
        return 1;
    }

    printf("Instância OK -> Vértices: %d | K: %d\n\n", g->n, k);
    
    // Execução Floyd-Warshall
    long long t_ini = tempo_ms();
    executar_floyd_warshall(g);
    long long t_fw = tempo_ms() - t_ini;
    printf("[FLOYD-WARSHALL] Concluído em %lld ms\n", t_fw);

    // Método Aproximado
    t_ini = tempo_ms();
    Resultado aprox = aproximado(g, k);
    long long t_aprox = tempo_ms() - t_ini;
    printf("[%s] Raio: %ld | Tempo: %lld ms\n", aprox.metodo, aprox.raio, t_aprox);

    // Método Exato
    printf("Iniciando varredura paralela global...\n");
    t_ini = tempo_ms();
    Resultado exato = exato_paralelo(g, k, aprox);
    long long t_exato = tempo_ms() - t_ini;
    
    printf("\n[%s] Raio Ótimo: %ld | Tempo: %lld ms\n", exato.metodo, exato.raio, t_exato);
    printf("Centros Selecionados: { ");
    for (int i = 0; i < k; i++) {
        printf("%d ", exato.centros[i] + 1); // +1 para voltar ao formato legível
    }
    printf("}\n");

    // Limpeza de memória
    free(g->dist);
    free(g);
    free(aprox.centros);
    free(exato.centros);

    return 0;
}