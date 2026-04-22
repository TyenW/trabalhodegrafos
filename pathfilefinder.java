//classe para ler e ver as pastas
class pathfilefinder{

    // as pastas seguem uam ordem
    //a primeira camada é ("grafos" + numero de vertices + tipo do grafo) sendi tipo: euriliano semieuriliano e nao euleriano.
    //a segunda camada sao pastas com grafos enumerados tipo grafo1 grafo2 etc
    //a terceira camada sao os arquivos de texto com os grafos: grafo.txt   

    

    //função para retorar um array de arquivos podendo selecionar o grafo desejado
    
    public static File[] encontrarArquivos(String tipoGrafo, int numVertices) {
        String caminhoBase = "grafos" + numVertices + tipoGrafo;
        File pastaBase = new File(caminhoBase);
        
        if (!pastaBase.exists() || !pastaBase.isDirectory()) {
            System.out.println("Pasta '" + caminhoBase + "' não encontrada.");
            return new File[0];
        }
        
        List<File> arquivosEncontrados = new ArrayList<>();
        
        // Percorre as subpastas (grafo1, grafo2, etc.)
        for (File subpasta : pastaBase.listFiles()) {
            if (subpasta.isDirectory()) {
                File arquivoGrafo = new File(subpasta, "grafo.txt");
                if (arquivoGrafo.exists()) {
                    arquivosEncontrados.add(arquivoGrafo);
                }
            }
        }
        
        return arquivosEncontrados.toArray(new File[0]);
    }


}