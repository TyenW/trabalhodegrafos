# Trabalho de Grafos

Projeto em Java para:
- gerar grafos nao direcionados classificados como euleriano, semieuleriano e naoeuleriano;
- gravar os grafos em disco (formato texto e formato binario compactado);
- ler e validar lotes de grafos gerados;
- executar busca de caminho euleriano (Fleury) com duas estrategias para deteccao de ponte (ingenua e Tarjan).

## Estrutura do repositorio

- `Main.java`: implementa estrutura de grafo em memoria, DFS, deteccao de pontes (naive e Tarjan) e caminho euleriano.
- `geradorgrafos.java`: gerador de lotes de grafos com menu interativo e escrita em `grafo.txt` e `grafo.fws`.
- `leitoreteste.java`: varredura de pastas geradas para validar consistencia e classificacao dos grafos.
- `pathfilefinder.java`: utilitario para localizar arquivos `grafo.txt` em pastas por tipo e quantidade de vertices.
- `grafo.txt` e `graph.txt`: exemplos de arquivos de entrada no formato texto.

## Requisitos

- Java JDK 17+ (recomendado 17 ou superior).
- Sistema operacional: Windows (tambem funciona em Linux/macOS com ajustes minimos de comando).

Para verificar:

```powershell
java -version
javac -version
```

## Como compilar

No PowerShell, dentro da pasta do projeto:

```powershell
javac Main.java geradorgrafos.java leitoreteste.java pathfilefinder.java
```

## Como executar

### 1) Gerar grafos em lote

```powershell
java geradorgrafos
```

Fluxo:
1. Escolher tipo:
   - 1: euleriano
   - 2: semieuleriano
   - 3: naoeuleriano
   - 4: todos os tipos e tamanhos
2. Escolher tamanho de vertices:
   - 1: 100
   - 2: 1000
   - 3: 10000
   - 4: 100000
3. Informar quantos grafos gerar.

Saida de arquivos:
- Pasta base: `grafos{N}{tipo}`
  - Exemplo: `grafos100euleriano`
- Subpastas: `grafo1`, `grafo2`, ...
- Arquivos por grafo:
  - `grafo.txt` (texto)
  - `grafo.fws` (binario compactado)

### 2) Validar grafos gerados

```powershell
java leitoreteste
```

O validador:
- procura pastas no padrao `grafos{N}{tipo}`;
- le todos os `grafo.txt` em subpastas `grafoN`;
- reclassifica cada grafo com base em conectividade e paridade;
- exibe resumo final com:
  - total analisado;
  - ok;
  - divergentes;
  - erros de leitura/formato.

### 3) Executar caminho euleriano (Fleury)

```powershell
java Main
```

Comportamento atual do `Main`:
- carrega fixamente `grafos100euleriano/grafo1/grafo.txt`;
- pede algoritmo de ponte:
  - 1: ingenuo
  - 2: Tarjan
- imprime sequencia de arestas do caminho euleriano;
- imprime tempo total de execucao.

## Formato de arquivo de entrada (`grafo.txt`)

Primeira linha:
- `N M`
  - `N`: numero de vertices
  - `M`: numero de arestas

Linhas seguintes:
- `u v` para cada aresta nao direcionada, com vertices em `[1..N]`.

Exemplo:

```txt
5 6
1 2
2 3
3 4
4 5
5 1
2 4
```

Observacao:
- o leitor interno de validacao tambem aceita linhas no formato `u-v`.

## Formato binario `grafo.fws` (resumo)

Escrito pelo gerador em `geradorgrafos.java`:
1. assinatura ASCII: `fws`
2. `vertices` em varint
3. `arestas` em varint
4. array de graus comprimidos por origem (varint)
5. destinos com delta encoding por origem (varint)

Objetivo:
- reduzir custo de I/O e armazenamento para grafos grandes.

## Visao tecnica dos algoritmos

### Geracao de grafos

Classe principal: `Grafo` (em `geradorgrafos.java`).

- Euleriano:
  - cria ciclo base conectando todos os vertices;
  - densifica em trios (triangulos) para preservar paridade dos graus;
  - reforca conectividade via DFS quando necessario.
- Semieuleriano:
  - cria caminho base (2 vertices impares);
  - densifica preservando paridade global;
  - reforca conectividade.
- Naoeuleriano:
  - cria caminho base;
  - adiciona aresta extra para quebrar condicao euleriana;
  - densifica e garante conectividade.

### Deteccao de pontes

Implementada em `Main.java`:

- `naive`:
  - remove aresta `(u,v)`;
  - recalcula componentes por DFS;
  - se componentes aumentam, a aresta e ponte;
  - restaura a aresta.

- `Tarjan`:
  - DFS com vetores de descoberta (`td`) e menor alcance (`low`);
  - aresta pai-filho e ponte quando `low[filho] > td[pai]`.

### Caminho Euleriano (Fleury)

Classe: `CaminhoEuleriano` em `Main.java`.

- em cada passo, escolhe aresta valida que nao seja ponte (quando possivel);
- remove aresta escolhida do grafo;
- segue recursivamente ate consumir as arestas.

## Classes principais

### Em `Main.java`

- `Vertice`: representa vertice com lista de vizinhos e grau.
- `Grafos`: leitura de arquivo e operacoes globais no grafo.
- `Arestas`: representa aresta classificada pela DFS.
- `DFS`: DFS geral e DFS de Tarjan.
- `naive`: deteccao de pontes por remocao e contagem de componentes.
- `Tarjan`: deteccao de pontes com low-link.
- `CaminhoEuleriano`: Fleury com estrategia de ponte configuravel.
- `Main`: ponto de entrada da execucao do caminho euleriano.

### Em `geradorgrafos.java`

- `geradorgrafos`: menu e orquestracao de lotes.
- `Grafo`: regras de geracao e conectividade.
- `fowardstar`, `No`, `listaduplamenteencadeada`: estrutura interna de adjacencia.
- `ResumoArestas`: metadados para serializacao.

### Em `leitoreteste.java`

- `leitoreteste`: varredura de diretorios, leitura, validacao estrutural e classificacao.

### Em `pathfilefinder.java`

- `pathfilefinder`: localiza arquivos `grafo.txt` por tipo/tamanho.

## Observacoes importantes

- O `Main.java` usa caminho fixo para leitura inicial do grafo:
  - `grafos100euleriano/grafo1/grafo.txt`
- Se essa pasta/arquivo nao existir, a execucao falha com `FileNotFoundException`.
- Recomenda-se gerar ao menos um lote antes de executar `Main`.

## Sugestao de fluxo de uso

1. Compilar o projeto.
2. Executar `java geradorgrafos` e criar um lote (ex.: 100 vertices euleriano).
3. Executar `java leitoreteste` para validar o lote.
4. Executar `java Main` para montar caminho euleriano comparando naive vs Tarjan.

## Melhorias recomendadas

- Parametrizar o caminho de entrada no `Main` via argumento de linha de comando.
- Unificar nomenclatura (`euriliano` vs `euleriano`) para reduzir ambiguidades.
- Adicionar testes automatizados para:
  - classificacao do tipo de grafo;
  - deteccao de pontes;
  - validade do caminho euleriano.
- Padronizar imports e validacao em `pathfilefinder.java`.

## Autores

- Filipe Nery
- Pedro Guimarães Alves Freitas

Se preferir, substitua os nomes acima por nome completo, email e/ou perfil GitHub de cada integrante.

## Licenca

Defina a licenca desejada para o projeto (ex.: MIT) e adicione o arquivo `LICENSE`.
