import java.util.Scanner;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;



class geradorgrafos{
    static final Map<String, Integer> proximoIndicePorPasta = new HashMap<String, Integer>();
    static final int IO_BUFFER_BYTES = 1 << 20;
    static final int[] OPCOES_TIPO = new int[]{1, 2, 3};
    static final int[] OPCOES_VERTICES = new int[]{1, 2, 3, 4};

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            int op1 = menu1(sc);
            if (op1 == 0) {
                System.out.println("Encerrando...");
                break;
            }

            if (op1 == 4) {
                limparTela();
                System.out.println("quantos grafos de CADA combinação deseja gerar?");
                int quantidadePorCombinacao = lerInteiroNoIntervalo(sc, 1, Integer.MAX_VALUE);

                for (int tipo : OPCOES_TIPO) {
                    for (int opcaoVertices : OPCOES_VERTICES) {
                        System.out.println();
                        System.out.println(">>> Gerando " + quantidadePorCombinacao + " grafos para tipo="
                            + tipoDeGrafoPorOpcao(tipo)
                            + " com " + verticesPorOpcao(opcaoVertices) + " vértices");
                        gerarLote(quantidadePorCombinacao, tipo, opcaoVertices);
                    }
                }
            } else {
                int op2 = menu2(sc);
                if (op2 == 0) {
                    continue;
                }

                limparTela();
                System.out.println("quantos grafos deseja gerar?");
                int quantidadeGrafos = lerInteiroNoIntervalo(sc, 1, Integer.MAX_VALUE);
                gerarLote(quantidadeGrafos, op1, op2);
            }
        }

        sc.close();
    }

    static void gerarLote(int quantidadeGrafos, int op1, int op2) {
        long inicioLote = System.currentTimeMillis();
        int vertices = verticesPorOpcao(op2);

        for (int i = 0; i < quantidadeGrafos; i++) {
            long inicioGrafo = System.currentTimeMillis();
            atualizarLoadingTerminalDetalhado(i, quantidadeGrafos, inicioLote, inicioGrafo, "iniciando", vertices, 0);

            Grafo grafo = new Grafo(vertices, 0);
            switch (op1) {
                case 1:
                    grafo = grafo.gerarGrafoEuriliano(vertices, 0);
                    break;
                case 2:
                    grafo = grafo.gerarGrafoSemiEuriliano(vertices, 0);
                    break;
                case 3:
                    grafo = grafo.gerarGrafoNaoEuriliano(vertices, 0);
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
            atualizarLoadingTerminalDetalhado(i, quantidadeGrafos, inicioLote, inicioGrafo, "gerado em memória", grafo.vertices, grafo.arestas);
            registrarGrafoEmArquivo(grafo, op1, i + 1, quantidadeGrafos, inicioLote, inicioGrafo);
            atualizarLoadingTerminalDetalhado(i + 1, quantidadeGrafos, inicioLote, inicioGrafo, "concluído", grafo.vertices, grafo.arestas);
        }

        if (quantidadeGrafos > 0) {
            System.out.println();
        }
    }

    static int verticesPorOpcao(int op2) {
        switch (op2) {
            case 1:
                return 100;
            case 2:
                return 1000;
            case 3:
                return 10000;
            case 4:
                return 100000;
            default:
                System.out.println("Opção inválida. Gerando grafo com 100 vértices por padrão.");
                return 100;
        }
    }

    static void registrarGrafoEmArquivo(Grafo grafo, int op1, int indiceAtual, int total, long inicioLote, long inicioGrafo) {
        String tipoDeGrafo = tipoDeGrafoPorOpcao(op1);
        String nomePastaBase = "grafos" + grafo.vertices + tipoDeGrafo;

        try {
            atualizarLoadingTerminalDetalhado(indiceAtual - 1, total, inicioLote, inicioGrafo, "criando diretórios", grafo.vertices, grafo.arestas);
            Path pastaBase = Paths.get(nomePastaBase);
            Files.createDirectories(pastaBase);

            int proximoIndice = obterProximoIndiceDeGrafo(pastaBase, nomePastaBase);
            Path pastaDoGrafo = pastaBase.resolve("grafo" + proximoIndice);
            Files.createDirectories(pastaDoGrafo);

            atualizarLoadingTerminalDetalhado(indiceAtual - 1, total, inicioLote, inicioGrafo, "contando arestas", grafo.vertices, grafo.arestas);
            ResumoArestas resumo = resumirArestasComprimidas(grafo);

            atualizarLoadingTerminalDetalhado(indiceAtual - 1, total, inicioLote, inicioGrafo, "escrevendo grafo.txt", grafo.vertices, resumo.totalArestasComprimidas);
            escreverArquivoTxt(pastaDoGrafo.resolve("grafo.txt"), grafo, resumo.totalArestasComprimidas);
            atualizarLoadingTerminalDetalhado(indiceAtual - 1, total, inicioLote, inicioGrafo, "escrevendo grafo.fws", grafo.vertices, resumo.totalArestasComprimidas);
            escreverArquivoFws(pastaDoGrafo.resolve("grafo.fws"), grafo, resumo);
        } catch (IOException e) {
            System.out.println("Erro ao registrar grafo: " + e.getMessage());
        }
    }

    static String tipoDeGrafoPorOpcao(int op1) {
        switch (op1) {
            case 1:
                return "euleriano";
            case 2:
                return "semieuleriano";
            case 3:
                return "naoeuleriano";
            default:
                return "desconhecido";
        }
    }

    static int obterProximoIndiceDeGrafo(Path pastaBase, String nomePastaBase) throws IOException {
        Integer cache = proximoIndicePorPasta.get(nomePastaBase);
        if (cache != null) {
            proximoIndicePorPasta.put(nomePastaBase, cache + 1);
            return cache;
        }

        int maiorIndice = 0;

        try (var stream = Files.list(pastaBase)) {
            for (Path caminho : (Iterable<Path>) stream::iterator) {
                if (!Files.isDirectory(caminho)) {
                    continue;
                }

                String nome = caminho.getFileName().toString();
                if (!nome.startsWith("grafo")) {
                    continue;
                }

                String sufixo = nome.substring("grafo".length());
                try {
                    int indice = Integer.parseInt(sufixo);
                    if (indice > maiorIndice) {
                        maiorIndice = indice;
                    }
                } catch (NumberFormatException e) {
                    // Ignora subpastas fora do padrão grafoN.
                }
            }
        }

        int proximo = maiorIndice + 1;
        proximoIndicePorPasta.put(nomePastaBase, proximo + 1);
        return proximo;
    }

    static ResumoArestas resumirArestasComprimidas(Grafo grafo) {
        int vertices = grafo.vertices;
        int[] grausComprimidos = new int[vertices + 1];
        int totalArestasComprimidas = 0;

        for (int origem = 1; origem <= vertices; origem++) {
            No atual = grafo.fowardstar.primeiroDestinoDaOrigem(origem);
            while (atual != null) {
                if (origem < atual.valor) {
                    grausComprimidos[origem]++;
                    totalArestasComprimidas++;
                }
                atual = atual.proximoOrigem;
            }
        }

        return new ResumoArestas(totalArestasComprimidas, grausComprimidos);
    }

    static void escreverArquivoTxt(Path caminhoArquivo, Grafo grafo, int totalArestasComprimidas) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(
                new BufferedOutputStream(Files.newOutputStream(caminhoArquivo), IO_BUFFER_BYTES),
                StandardCharsets.UTF_8
            ),
            IO_BUFFER_BYTES
        )) {
            writer.write(grafo.vertices + " " + totalArestasComprimidas);
            writer.newLine();

            for (int origem = 1; origem <= grafo.vertices; origem++) {
                No atual = grafo.fowardstar.primeiroDestinoDaOrigem(origem);
                while (atual != null) {
                    int destino = atual.valor;
                    if (origem < destino) {
                        writer.write(origem + " " + destino);
                        writer.newLine();
                    }
                    atual = atual.proximoOrigem;
                }
            }
        }
    }

    static void escreverArquivoFws(Path caminhoArquivo, Grafo grafo, ResumoArestas resumo) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(caminhoArquivo), IO_BUFFER_BYTES))) {
            out.writeBytes("fws");
            escreverVarInt(out, grafo.vertices);
            escreverVarInt(out, resumo.totalArestasComprimidas);

            // Array de graus (quantidade de destinos por origem).
            for (int origem = 1; origem <= grafo.vertices; origem++) {
                escreverVarInt(out, resumo.grausComprimidos[origem]);
            }

            // Array de destinos com delta encoding por origem.
            for (int origem = 1; origem <= grafo.vertices; origem++) {
                int quantidade = resumo.grausComprimidos[origem];
                int[] destinos = new int[quantidade];
                int indice = 0;

                No atual = grafo.fowardstar.primeiroDestinoDaOrigem(origem);
                while (atual != null) {
                    if (origem < atual.valor) {
                        destinos[indice++] = atual.valor;
                    }
                    atual = atual.proximoOrigem;
                }

                if (quantidade > 1) {
                    java.util.Arrays.sort(destinos);
                }

                int anterior = origem;
                for (int i = 0; i < quantidade; i++) {
                    int destino = destinos[i];
                    int delta = destino - anterior;
                    escreverVarInt(out, delta);
                    anterior = destino;
                }
            }
        }
    }

    static void escreverVarInt(DataOutputStream out, int valor) throws IOException {
        while ((valor & ~0x7F) != 0) {
            out.writeByte((valor & 0x7F) | 0x80);
            valor >>>= 7;
        }
        out.writeByte(valor);
    }

    static void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void atualizarLoadingTerminalDetalhado(int concluido, int total, long inicioLote, long inicioGrafo, String fase, int vertices, int arestas) {
        if (total <= 0) {
            return;
        }

        int larguraBarra = 30;
        int preenchido = (int) (((long) concluido * larguraBarra) / total);
        int percentual = (int) (((long) concluido * 100) / total);
        long agora = System.currentTimeMillis();
        long decorridoMs = agora - inicioLote;
        long tempoGrafoMs = agora - inicioGrafo;
        long etaMs = 0;
        if (concluido > 0) {
            etaMs = (decorridoMs / concluido) * (total - concluido);
        }

        StringBuilder barra = new StringBuilder();
        barra.append('[');
        for (int i = 0; i < larguraBarra; i++) {
            barra.append(i < preenchido ? '=' : '.');
        }
        barra.append(']');

        String linha = String.format(
            "\rGerando %s %3d%% (%d/%d) | fase: %s | V:%d A:%d | item:%d ms | lote:%d ms | ETA:%d ms",
            barra,
            percentual,
            concluido,
            total,
            fase,
            vertices,
            arestas,
            tempoGrafoMs,
            decorridoMs,
            etaMs
        );
        System.out.print(linha);
        System.out.flush();
    }

   static int menu1(Scanner scanner) {
        limparTela();
        System.out.println("Menu:");
        System.out.println("1. Gerar grafo Euriliano");
        System.out.println("2. Gerar grafo Semi-Euriliano");
        System.out.println("3. Gerar grafo não Euriliano");
       System.out.println("4. Gerar TODOS (3 tipos x 4 tamanhos)");
        System.out.println("0. Sair");
       return lerInteiroNoIntervalo(scanner, 0, 4);
    }

    static int menu2(Scanner scanner) {
        //100, 1.000, 10.000 e 100.000 vértices.
        limparTela();
        System.out.println("Menu:");
        System.out.println("1. Gerar grafo com 100 vértices");
        System.out.println("2. Gerar grafo com 1.000 vértices");
        System.out.println("3. Gerar grafo com 10.000 vértices");
        System.out.println("4. Gerar grafo com 100.000 vértices");
        System.out.println("0. Voltar");
        return lerInteiroNoIntervalo(scanner, 0, 4);
    }

    static int lerInteiroNoIntervalo(Scanner scanner, int minimo, int maximo) {
        while (true) {
            if (!scanner.hasNextInt()) {
                System.out.println("Entrada inválida. Digite um número:");
                scanner.next();
                continue;
            }

            int valor = scanner.nextInt();
            if (valor < minimo || valor > maximo) {
                System.out.println("Opção inválida. Digite novamente:");
                continue;
            }

            return valor;
        }
    }
}

class ArestaArquivo implements Comparable<ArestaArquivo> {
    int origem;
    int destino;

    ArestaArquivo(int origem, int destino) {
        this.origem = origem;
        this.destino = destino;
    }

    @Override
    public int compareTo(ArestaArquivo outra) {
        if (this.origem != outra.origem) {
            return Integer.compare(this.origem, outra.origem);
        }
        return Integer.compare(this.destino, outra.destino);
    }
}

class ResumoArestas {
    int totalArestasComprimidas;
    int[] grausComprimidos;

    ResumoArestas(int totalArestasComprimidas, int[] grausComprimidos) {
        this.totalArestasComprimidas = totalArestasComprimidas;
        this.grausComprimidos = grausComprimidos;
    }
}


class Grafo{
    int vertices;
    int arestas;
    fowardstar fowardstar;

    Grafo(int vertices, int arestas){
        this.vertices = vertices;
        this.arestas = arestas;
        this.fowardstar = new fowardstar( vertices);
    }

   Grafo gerarGrafoEuriliano(int vertices, int arestas){
        SecureRandom secureRandom = new SecureRandom();
        Grafo grafo = new Grafo(vertices, arestas);

        if (vertices <= 0) {
            return grafo;
        }

        int[] deficit = new int[vertices + 1];
        int maxGrauBase = calcularMaxGrauBase(vertices);

        // Gera um grau aleatório para cada vértice e força o grau a ser par.
        int verticesComDeficit = 0;
        for (int i = 1; i <= vertices; i++) {
            int grauAleatorio = secureRandom.nextInt(maxGrauBase + 1);
            if (grauAleatorio % 2 != 0) {
                grauAleatorio++;
            }
            deficit[i] = grauAleatorio;
            if (grauAleatorio > 0) {
                verticesComDeficit++;
            }
        }

        int arestasInseridas = 0;

        while (verticesComDeficit > 0) {
            int origem = sortearVerticeComDeficit(deficit, secureRandom);
            if (origem == -1) {
                break;
            }

            int destino;

            if (verticesComDeficit == 1 && deficit[origem] >= 2) {
                // Caso de único vértice restante: usa auto-laço para reduzir 2 no grau.
                destino = origem;
            } else {
                destino = sortearOutroVerticeComDeficit(deficit, origem, secureRandom);
                if (destino == -1) {
                    // Sem par disponível: usa auto-laço para manter paridade.
                    destino = origem;
                }
            }

            grafo = adicionarArestaNaoDirecionada(grafo, origem, destino);
            arestasInseridas++;

            if (origem == destino) {
                int antesOrigem = deficit[origem];
                int depoisOrigem = antesOrigem - 2;
                if (depoisOrigem < 0) {
                    depoisOrigem = 0;
                }
                deficit[origem] = depoisOrigem;
                if (antesOrigem > 0 && depoisOrigem == 0) {
                    verticesComDeficit--;
                }
            } else {
                int antesOrigem = deficit[origem];
                int depoisOrigem = antesOrigem - 1;
                if (depoisOrigem < 0) {
                    depoisOrigem = 0;
                }
                deficit[origem] = depoisOrigem;
                if (antesOrigem > 0 && depoisOrigem == 0) {
                    verticesComDeficit--;
                }

                int antesDestino = deficit[destino];
                int depoisDestino = antesDestino - 1;
                if (depoisDestino < 0) {
                    depoisDestino = 0;
                }
                deficit[destino] = depoisDestino;
                if (antesDestino > 0 && depoisDestino == 0) {
                    verticesComDeficit--;
                }
            }
        }

        grafo.arestas = arestasInseridas;
        return grafo;
    }

    private Grafo adicionarArestaNaoDirecionada(Grafo grafo, int origem, int destino) {
        grafo.fowardstar = grafo.fowardstar.inserirAresta(grafo.fowardstar, origem, destino);
        grafo.fowardstar = grafo.fowardstar.inserirAresta(grafo.fowardstar, destino, origem);
        return grafo;
    }

    private int contarVerticesComDeficit(int[] deficit) {
        int total = 0;
        for (int i = 1; i < deficit.length; i++) {
            if (deficit[i] > 0) {
                total++;
            }
        }
        return total;
    }

    private int sortearVerticeComDeficit(int[] deficit, SecureRandom random) {
        int tentativas = deficit.length * 3;
        while (tentativas-- > 0) {
            int candidato = random.nextInt(deficit.length - 1) + 1;
            if (deficit[candidato] > 0) {
                return candidato;
            }
        }

        for (int i = 1; i < deficit.length; i++) {
            if (deficit[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    private int sortearOutroVerticeComDeficit(int[] deficit, int origem, SecureRandom random) {
        int tentativas = deficit.length * 3;
        while (tentativas-- > 0) {
            int candidato = random.nextInt(deficit.length - 1) + 1;
            if (candidato != origem && deficit[candidato] > 0) {
                return candidato;
            }
        }

        for (int i = 1; i < deficit.length; i++) {
            if (i != origem && deficit[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    Grafo gerarGrafoSemiEuriliano(int vertices, int arestas){
        SecureRandom secureRandom = new SecureRandom();
        Grafo grafo = new Grafo(vertices, arestas);

        if (vertices <= 0) {
            return grafo;
        }

        int[] graus = new int[vertices + 1];
        int maxGrauBase = calcularMaxGrauBase(vertices);

        // Sorteia o grau-base de cada vértice.
        for (int i = 1; i <= vertices; i++) {
            graus[i] = secureRandom.nextInt(maxGrauBase + 1);
        }

        // Em modo semi-euleriano, força exatamente 2 vértices ímpares.
        int alvoImpares = (vertices >= 2) ? 2 : 0;

        // Ajusta paridade para ficar com no máximo dois vértices ímpares.
        List<Integer> impares = listarVerticesImpares(graus);
        while (impares.size() > alvoImpares) {
            int vertice = impares.remove(impares.size() - 1);
            graus[vertice]++;
        }
        while (impares.size() < alvoImpares) {
            int vertice = sortearVerticePar(graus, secureRandom);
            if (vertice == -1) {
                break;
            }
            graus[vertice]++;
            impares.add(vertice);
        }

        // Monta stubs (repetições de vértices por grau) e emparelha aleatoriamente.
        int[] stubs = montarStubs(graus, vertices);
        int arestasInseridas = emparelharStubsEmLote(stubs, secureRandom, grafo);

        grafo.arestas = arestasInseridas;
        return grafo;
    }

    private List<Integer> listarVerticesImpares(int[] graus) {
        List<Integer> impares = new ArrayList<Integer>();
        for (int i = 1; i < graus.length; i++) {
            if (graus[i] % 2 != 0) {
                impares.add(i);
            }
        }
        return impares;
    }

    private int sortearVerticePar(int[] graus, SecureRandom random) {
        int tentativas = graus.length * 3;
        while (tentativas-- > 0) {
            int candidato = random.nextInt(graus.length - 1) + 1;
            if (graus[candidato] % 2 == 0) {
                return candidato;
            }
        }

        for (int i = 1; i < graus.length; i++) {
            if (graus[i] % 2 == 0) {
                return i;
            }
        }
        return -1;
    }

    private int[] montarStubs(int[] graus, int vertices) {
        int totalStubs = 0;
        for (int i = 1; i <= vertices; i++) {
            totalStubs += graus[i];
        }

        int[] stubs = new int[totalStubs];
        int indice = 0;
        for (int i = 1; i <= vertices; i++) {
            for (int j = 0; j < graus[i]; j++) {
                stubs[indice++] = i;
            }
        }
        return stubs;
    }

    private int emparelharStubsEmLote(int[] stubs, Random random, Grafo grafo) {
        for (int i = stubs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = stubs[i];
            stubs[i] = stubs[j];
            stubs[j] = tmp;
        }

        int arestasInseridas = 0;

        for (int i = 0; i + 1 < stubs.length; i += 2) {
            int origem = stubs[i];
            int destino = stubs[i + 1];
            adicionarArestaNaoDirecionada(grafo, origem, destino);
            arestasInseridas++;
        }

        return arestasInseridas;
    }

    Grafo gerarGrafoNaoEuriliano(int vertices, int arestas){
        SecureRandom secureRandom = new SecureRandom();
        Grafo grafo = new Grafo(vertices, arestas);

        if (vertices < 4) {
            throw new IllegalArgumentException("Para gerar grafo nao euleriano, use pelo menos 4 vertices.");
        }

        int[] graus = new int[vertices + 1];
        int maxGrauBase = calcularMaxGrauBase(vertices);

        // Sorteia o grau-base de cada vértice.
        for (int i = 1; i <= vertices; i++) {
            graus[i] = secureRandom.nextInt(maxGrauBase + 1);
        }

        // Em grafo não direcionado, a quantidade de ímpares deve ser par.
        int maxImpares = (vertices % 2 == 0) ? vertices : vertices - 1;
        int quantidadeOpcoes = ((maxImpares - 4) / 2) + 1;
        int alvoImpares = 4 + (2 * secureRandom.nextInt(quantidadeOpcoes));

        // Ajusta paridade para ficar com mais de dois vértices ímpares.
        List<Integer> impares = listarVerticesImpares(graus);
        while (impares.size() > alvoImpares) {
            int vertice = impares.remove(impares.size() - 1);
            graus[vertice]++;
        }
        while (impares.size() < alvoImpares) {
            int vertice = sortearVerticePar(graus, secureRandom);
            if (vertice == -1) {
                break;
            }
            graus[vertice]++;
            impares.add(vertice);
        }

        // Monta stubs (repetições de vértices por grau) e emparelha aleatoriamente.
        int[] stubs = montarStubs(graus, vertices);
        int arestasInseridas = emparelharStubsEmLote(stubs, secureRandom, grafo);

        grafo.arestas = arestasInseridas;
        return grafo;
    }

    private int calcularMaxGrauBase(int vertices) {
        int limite = vertices - 1;
        if (limite < 2) {
            return 2;
        }

        // Em grafos muito grandes, reduz grau-alvo para diminuir custo total de geração e I/O.
        if (vertices >= 100000) {
            return Math.min(limite, 8);
        }
        if (vertices >= 10000) {
            return Math.min(limite, 12);
        }
        return Math.min(limite, 20);
    }


}

class fowardstar{
    listaduplamenteencadeada ponteiro;
    listaduplamenteencadeada destino;
    No[] indicePonteiroPorVertice;
    No[] inicioDestinoPorOrigem;
    No[] fimDestinoPorOrigem;
    int[] grauPorOrigem;

    fowardstar(int vertices){
        this.indicePonteiroPorVertice = new No[Math.max(0, vertices + 1)];
        this.inicioDestinoPorOrigem = new No[Math.max(0, vertices + 1)];
        this.fimDestinoPorOrigem = new No[Math.max(0, vertices + 1)];
        this.grauPorOrigem = new int[Math.max(0, vertices + 1)];
        this.ponteiro = montarponteiro(vertices);
        this.destino = new listaduplamenteencadeada();
    }

    fowardstar likarponteiroscomdestino(fowardstar fowardstar){
        //aqui apos rodar inserção quero garantir que em ponteiros o no.referencia seja a o primeiro no com o valor do no.referencia.valor igual ao valor do no.valor do ponteiro
        No atualPonteiro = fowardstar.ponteiro.inicio;
        while (atualPonteiro != null) {
            No atualDestino = fowardstar.destino.inicio;
            while (atualDestino != null) {
                if (atualDestino.referencia != null && atualDestino.referencia.valor == atualPonteiro.valor) {
                    atualPonteiro.referencia = atualDestino;
                    break; // Encontrou o destino correspondente, pode sair do loop
                }

                
                atualDestino = atualDestino.proximo;
            }
            atualPonteiro = atualPonteiro.proximo;
        }
        fowardstar.ponteiro = ponteiro;
        return fowardstar;

    }



    int verificargraudovertice(int vertice){
        if (vertice < 1 || vertice >= grauPorOrigem.length) {
            return -1;
        }
        return grauPorOrigem[vertice];
    }

    fowardstar inserirAresta(fowardstar fowardstar, int origem, int destino){
        No referencia = obterNoPonteiro(origem);
        if (referencia == null) {
            return fowardstar;
        }

        No novoNo = new No(destino, null, null, referencia);

        // Inserção O(1) no fim da lista de destinos.
        if(fowardstar.destino.inicio == null){
            fowardstar.destino.inicio = novoNo;
            fowardstar.destino.fim = novoNo;
        }else{
            fowardstar.destino.fim.proximo = novoNo;
            novoNo.anterior = fowardstar.destino.fim;
            fowardstar.destino.fim = novoNo;
        }

        // Mantém referência do primeiro destino de cada vértice de origem.
        if (referencia.referencia == null) {
            referencia.referencia = novoNo;
        }

        if (inicioDestinoPorOrigem[origem] == null) {
            inicioDestinoPorOrigem[origem] = novoNo;
            fimDestinoPorOrigem[origem] = novoNo;
        } else {
            fimDestinoPorOrigem[origem].proximoOrigem = novoNo;
            fimDestinoPorOrigem[origem] = novoNo;
        }
        grauPorOrigem[origem]++;

        return fowardstar;
    }

    No primeiroDestinoDaOrigem(int origem) {
        if (origem < 1 || origem >= inicioDestinoPorOrigem.length) {
            return null;
        }
        return inicioDestinoPorOrigem[origem];
    }

    private No obterNoPonteiro(int vertice) {
        if (vertice >= 0 && vertice < indicePonteiroPorVertice.length) {
            return indicePonteiroPorVertice[vertice];
        }

        No referencia = ponteiro.inicio;
        while (referencia != null && referencia.valor != vertice) {
            referencia = referencia.proximo;
        }
        return referencia;
    }
    

    listaduplamenteencadeada montarponteiro(int vertices){
        listaduplamenteencadeada ponteiros = new listaduplamenteencadeada();

        if (vertices <= 0) {
            return ponteiros;
        }

        No indice = new No(1, null, null, null);
        ponteiros.inicio = indice;
        indicePonteiroPorVertice[1] = indice;

        for (int i = 2; i <= vertices; i++) {
            No novoNo = new No(i, indice, null, null);
            indice.proximo = novoNo;
            indice = novoNo;
            indicePonteiroPorVertice[i] = novoNo;
        }

        ponteiros.fim = indice;
        return ponteiros;
    }
}

//lista usada para foawardstar 
class listaduplamenteencadeada{
   
        No inicio;
            No fim;
    
            public listaduplamenteencadeada() {
                this.inicio = null;
                this.fim = null;
            }
    
           
       

  
}
  

class No {
        int valor;
        No proximo;
        No proximoOrigem;
        No anterior;
        No referencia;

        public No(int valor,No anterior, No proximo, No referencia) {
            this.valor = valor;
            this.proximo = proximo;
            this.proximoOrigem = null;
            this.anterior = anterior;
            this.referencia = referencia;
        }
        //metodo clone 
        public No clone() {
            return new No(this.valor, this.anterior, this.proximo, this.referencia);
        }

    }