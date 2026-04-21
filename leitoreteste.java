import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class leitoreteste {
	private static final Pattern PASTA_BASE_PATTERN = Pattern.compile("^grafos(\\d+)(euleriano|semieuleriano|naoeuleriano)$");
	private static final Pattern SUBPASTA_GRAFO_PATTERN = Pattern.compile("^grafo\\d+$");

	public static void main(String[] args) {
		Path raiz = Paths.get(".").toAbsolutePath().normalize();
		System.out.println("Validando grafos em: " + raiz);

		List<Path> pastasBase = listarPastasBase(raiz);
		if (pastasBase.isEmpty()) {
			System.out.println("Nenhuma pasta no formato grafos{N}{tipo} foi encontrada.");
			return;
		}

		int totalGrafos = 0;
		int totalOk = 0;
		int totalDivergente = 0;
		int totalComErro = 0;

		for (Path pastaBase : pastasBase) {
			String nomePasta = pastaBase.getFileName().toString();
			Matcher matcher = PASTA_BASE_PATTERN.matcher(nomePasta);
			if (!matcher.matches()) {
				continue;
			}

			int verticesEsperados = Integer.parseInt(matcher.group(1));
			String tipoEsperado = matcher.group(2);

			System.out.println();
			System.out.println("Pasta base: " + nomePasta + " | esperado: V=" + verticesEsperados + ", tipo=" + tipoEsperado);

			List<Path> subpastasGrafos;
			try {
				subpastasGrafos = listarSubpastasGrafo(pastaBase);
			} catch (IOException e) {
				totalComErro++;
				System.out.println("  - ERRO ao listar subpastas: " + e.getMessage());
				continue;
			}
			if (subpastasGrafos.isEmpty()) {
				System.out.println("  - Nenhuma subpasta grafoN encontrada.");
				continue;
			}

			for (Path subpasta : subpastasGrafos) {
				totalGrafos++;
				Path arquivoTxt = subpasta.resolve("grafo.txt");

				if (!Files.exists(arquivoTxt)) {
					totalComErro++;
					System.out.println("  - " + subpasta.getFileName() + ": ERRO | arquivo grafo.txt não encontrado");
					continue;
				}

				try {
					ResultadoLeitura resultado = lerEClassificarGrafo(arquivoTxt);

					boolean verticesOk = (resultado.vertices == verticesEsperados);
					boolean tipoOk = resultado.classificacao.equals(tipoEsperado);

					if (verticesOk && tipoOk) {
						totalOk++;
						
					} else {
						totalDivergente++;
						System.out.println(
							"  - " + subpasta.getFileName()
							+ ": DIVERGENTE | tipo detectado=" + resultado.classificacao
							+ " (esperado=" + tipoEsperado + ")"
							+ " | V detectado=" + resultado.vertices
							+ " (esperado=" + verticesEsperados + ")"
							+ " | M=" + resultado.arestas
						);
					}
				} catch (Exception e) {
					totalComErro++;
					System.out.println("  - " + subpasta.getFileName() + ": ERRO | " + e.getMessage());
				}
			}
		}

		System.out.println();
		System.out.println("Resumo final");
		System.out.println("- Grafos analisados: " + totalGrafos);
		System.out.println("- OK: " + totalOk);
		System.out.println("- Divergentes: " + totalDivergente);
		System.out.println("- Erros de leitura/formato: " + totalComErro);
	}

	private static List<Path> listarPastasBase(Path raiz) {
		List<Path> pastas = new ArrayList<Path>();
		try (Stream<Path> stream = Files.list(raiz)) {
			for (Path caminho : (Iterable<Path>) stream::iterator) {
				if (!Files.isDirectory(caminho)) {
					continue;
				}
				String nome = caminho.getFileName().toString();
				if (PASTA_BASE_PATTERN.matcher(nome).matches()) {
					pastas.add(caminho);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Falha ao listar diretório raiz: " + e.getMessage(), e);
		}
		return pastas;
	}

	private static List<Path> listarSubpastasGrafo(Path pastaBase) throws IOException {
		List<Path> subpastas = new ArrayList<Path>();
		try (Stream<Path> stream = Files.list(pastaBase)) {
			for (Path caminho : (Iterable<Path>) stream::iterator) {
				if (!Files.isDirectory(caminho)) {
					continue;
				}
				String nome = caminho.getFileName().toString();
				if (SUBPASTA_GRAFO_PATTERN.matcher(nome).matches()) {
					subpastas.add(caminho);
				}
			}
		}
		return subpastas;
	}

	private static ResultadoLeitura lerEClassificarGrafo(Path arquivoTxt) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(arquivoTxt)) {
			String cabecalho = reader.readLine();
			if (cabecalho == null || cabecalho.trim().isEmpty()) {
				throw new IOException("arquivo vazio");
			}

			String[] partes = cabecalho.trim().split("\\s+");
			if (partes.length < 2) {
				throw new IOException("cabeçalho inválido: esperado 'N M'");
			}

			int n = Integer.parseInt(partes[0]);
			int mCabecalho = Integer.parseInt(partes[1]);
			if (n <= 0) {
				throw new IOException("número de vértices inválido: " + n);
			}

			UnionFind uf = new UnionFind(n + 1);
			int[] grau = new int[n + 1];
			IntBag[] adj = new IntBag[n + 1];
			for (int i = 1; i <= n; i++) {
				adj[i] = new IntBag();
			}

			int capacidadeInicial = Math.max(1, mCabecalho);
			int[] edgeU = new int[capacidadeInicial];
			int[] edgeV = new int[capacidadeInicial];
			boolean[] edgeLoop = new boolean[capacidadeInicial];

			int mLido = 0;
			String linha;
			while ((linha = reader.readLine()) != null) {
				linha = linha.trim();
				if (linha.isEmpty()) {
					continue;
				}

				String[] uv;
				if (linha.contains("-")) {
					uv = linha.split("-");
				} else {
					uv = linha.split("\\s+");
				}
				if (uv.length != 2) {
					throw new IOException("aresta inválida: '" + linha + "'");
				}

				int u = Integer.parseInt(uv[0].trim());
				int v = Integer.parseInt(uv[1].trim());
				if (u < 1 || u > n || v < 1 || v > n) {
					throw new IOException("aresta fora do intervalo de vértices: '" + linha + "'");
				}

				if (mLido == edgeU.length) {
					int novaCapacidade = edgeU.length * 2;
					edgeU = java.util.Arrays.copyOf(edgeU, novaCapacidade);
					edgeV = java.util.Arrays.copyOf(edgeV, novaCapacidade);
					edgeLoop = java.util.Arrays.copyOf(edgeLoop, novaCapacidade);
				}
				edgeU[mLido] = u;
				edgeV[mLido] = v;
				edgeLoop[mLido] = (u == v);
				adj[u].add(mLido);
				if (u != v) {
					adj[v].add(mLido);
				}

				if (u == v) {
					grau[u] += 2;
				} else {
					grau[u]++;
					grau[v]++;
					uf.union(u, v);
				}
				mLido++;
			}

			boolean conexoEntreVerticesComAresta = true;
			int representante = -1;
			for (int v = 1; v <= n; v++) {
				if (grau[v] == 0) {
					continue;
				}
				int raiz = uf.find(v);
				if (representante == -1) {
					representante = raiz;
				} else if (representante != raiz) {
					conexoEntreVerticesComAresta = false;
					break;
				}
			}

			int impares = 0;
			for (int v = 1; v <= n; v++) {
				if ((grau[v] & 1) != 0) {
					impares++;
				}
			}

			int pontes = 0;
			if (conexoEntreVerticesComAresta && impares == 0) {
				pontes = contarPontes(n, grau, adj, edgeU, edgeV, edgeLoop, mLido);
			}

			String classificacao;
			if (!conexoEntreVerticesComAresta) {
				classificacao = "naoeuleriano";
			} else if (impares == 0 && pontes == 0) {
				classificacao = "euleriano";
			} else if (impares == 2) {
				classificacao = "semieuleriano";
			} else {
				classificacao = "naoeuleriano";
			}

			if (mCabecalho != mLido) {
				// Não interrompe: apenas mantém o valor real para auditoria.
				mCabecalho = mLido;
			}

			return new ResultadoLeitura(n, mLido, classificacao);
		}
	}

	private static int contarPontes(int n, int[] grau, IntBag[] adj, int[] edgeU, int[] edgeV, boolean[] edgeLoop, int totalArestas) {
		int[] disc = new int[n + 1];
		int[] low = new int[n + 1];
		int[] parent = new int[n + 1];
		int[] parentEdge = new int[n + 1];
		int[] itIndex = new int[n + 1];
		int[] pilha = new int[n + 1];
		java.util.Arrays.fill(parentEdge, -1);

		int tempo = 0;
		int pontes = 0;

		for (int inicio = 1; inicio <= n; inicio++) {
			if (grau[inicio] == 0 || disc[inicio] != 0) {
				continue;
			}

			int topo = 0;
			pilha[topo++] = inicio;

			while (topo > 0) {
				int v = pilha[topo - 1];
				if (disc[v] == 0) {
					disc[v] = ++tempo;
					low[v] = disc[v];
				}

				if (itIndex[v] < adj[v].size()) {
					int edgeId = adj[v].get(itIndex[v]++);
					if (edgeId < 0 || edgeId >= totalArestas || edgeLoop[edgeId]) {
						continue;
					}

					int a = edgeU[edgeId];
					int b = edgeV[edgeId];
					int to = (a == v) ? b : a;

					if (disc[to] == 0) {
						parent[to] = v;
						parentEdge[to] = edgeId;
						pilha[topo++] = to;
					} else if (edgeId != parentEdge[v]) {
						if (disc[to] < low[v]) {
							low[v] = disc[to];
						}
					}
				} else {
					topo--;
					int p = parent[v];
					if (p != 0) {
						if (low[v] < low[p]) {
							low[p] = low[v];
						}
						if (low[v] > disc[p]) {
							pontes++;
						}
					}
				}
			}
		}

		return pontes;
	}

	private static class ResultadoLeitura {
		int vertices;
		int arestas;
		String classificacao;

		ResultadoLeitura(int vertices, int arestas, String classificacao) {
			this.vertices = vertices;
			this.arestas = arestas;
			this.classificacao = classificacao;
		}
	}

	private static class UnionFind {
		int[] parent;
		byte[] rank;

		UnionFind(int tamanho) {
			this.parent = new int[tamanho];
			this.rank = new byte[tamanho];
			for (int i = 0; i < tamanho; i++) {
				parent[i] = i;
			}
		}

		int find(int x) {
			if (parent[x] != x) {
				parent[x] = find(parent[x]);
			}
			return parent[x];
		}

		void union(int a, int b) {
			int ra = find(a);
			int rb = find(b);
			if (ra == rb) {
				return;
			}

			if (rank[ra] < rank[rb]) {
				parent[ra] = rb;
			} else if (rank[ra] > rank[rb]) {
				parent[rb] = ra;
			} else {
				parent[rb] = ra;
				rank[ra]++;
			}
		}
	}

	private static class IntBag {
		private int[] dados;
		private int tamanho;

		IntBag() {
			this.dados = new int[4];
			this.tamanho = 0;
		}

		void add(int valor) {
			if (tamanho == dados.length) {
				dados = java.util.Arrays.copyOf(dados, dados.length * 2);
			}
			dados[tamanho++] = valor;
		}

		int get(int indice) {
			return dados[indice];
		}

		int size() {
			return tamanho;
		}
	}
}
