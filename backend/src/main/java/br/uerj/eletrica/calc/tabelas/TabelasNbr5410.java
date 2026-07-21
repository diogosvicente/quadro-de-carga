package br.uerj.eletrica.calc.tabelas;

import br.uerj.eletrica.calc.CalculoException;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.TreeMap;

/**
 * Tabelas normativas da NBR 5410 carregadas de resources/nbr5410/*.json
 * (extraídas da planilha do departamento — ver docs/dados-normativos/).
 * Classe pura: sem Spring; instanciada uma vez via {@link #carregarDoClasspath()}.
 */
public final class TabelasNbr5410 {

    public record FaixaAgrupamento(int minCircuitos, Integer maxCircuitos, double fator) {}

    public record FormaAgrupamento(int ref, String descricao, List<FaixaAgrupamento> faixas) {}

    private final Map<Integer, FormaAgrupamento> formasAgrupamento;
    private final Map<Integer, double[]> temperaturaAmbiente; // temp -> [pvc, epr]
    private final Map<Integer, double[]> temperaturaSolo;
    private final List<Integer> disjuntores;                  // nominais padrão, crescente
    private final Map<Isolante, List<Double>> secoes;         // seções comerciais, crescente
    private final Map<Isolante, Map<MetodoInstalacao, Map<String, List<Double>>>> capacidades;
    private final TreeMap<Double, Double> neutro;             // fase -> neutro
    private final TreeMap<Double, Double> terra;              // fase -> PE

    private TabelasNbr5410(Map<Integer, FormaAgrupamento> formasAgrupamento,
                           Map<Integer, double[]> temperaturaAmbiente,
                           Map<Integer, double[]> temperaturaSolo,
                           List<Integer> disjuntores,
                           Map<Isolante, List<Double>> secoes,
                           Map<Isolante, Map<MetodoInstalacao, Map<String, List<Double>>>> capacidades,
                           TreeMap<Double, Double> neutro,
                           TreeMap<Double, Double> terra) {
        this.formasAgrupamento = formasAgrupamento;
        this.temperaturaAmbiente = temperaturaAmbiente;
        this.temperaturaSolo = temperaturaSolo;
        this.disjuntores = disjuntores;
        this.secoes = secoes;
        this.capacidades = capacidades;
        this.neutro = neutro;
        this.terra = terra;
    }

    // ── Consultas ──────────────────────────────────────────────────────────────

    public List<FormaAgrupamento> formasAgrupamento() {
        return List.copyOf(formasAgrupamento.values());
    }

    /** Fator de agrupamento (Tab. 42) para a forma {@code ref} e {@code n} circuitos agrupados. */
    public OptionalDouble fatorAgrupamento(int ref, int n) {
        FormaAgrupamento forma = formasAgrupamento.get(ref);
        if (forma == null) {
            return OptionalDouble.empty();
        }
        return forma.faixas().stream()
                .filter(f -> n >= f.minCircuitos() && (f.maxCircuitos() == null || n <= f.maxCircuitos()))
                .mapToDouble(FaixaAgrupamento::fator)
                .findFirst();
    }

    /** Fator de correção de temperatura (Tab. 40); exige temperatura exatamente tabelada. */
    public OptionalDouble fatorTemperatura(int temperaturaC, Isolante isolante, boolean subterranea) {
        double[] par = (subterranea ? temperaturaSolo : temperaturaAmbiente).get(temperaturaC);
        if (par == null) {
            return OptionalDouble.empty();
        }
        double valor = isolante == Isolante.PVC ? par[0] : par[1];
        return Double.isNaN(valor) ? OptionalDouble.empty() : OptionalDouble.of(valor);
    }

    public List<Integer> temperaturasTabeladas(boolean subterranea) {
        return (subterranea ? temperaturaSolo : temperaturaAmbiente).keySet().stream().sorted().toList();
    }

    /** Temperaturas com fator de correção existente para o isolante dado (para mensagens de erro). */
    public List<Integer> temperaturasTabeladas(Isolante isolante, boolean subterranea) {
        Map<Integer, double[]> tabela = subterranea ? temperaturaSolo : temperaturaAmbiente;
        return tabela.entrySet().stream()
                .filter(e -> !Double.isNaN(isolante == Isolante.PVC ? e.getValue()[0] : e.getValue()[1]))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public List<Integer> disjuntoresPadrao() {
        return disjuntores;
    }

    /** Menor corrente nominal padrão ≥ {@code correnteA}. */
    public OptionalInt menorDisjuntorMaiorIgual(double correnteA) {
        return disjuntores.stream().filter(d -> d >= correnteA).mapToInt(Integer::intValue).findFirst();
    }

    /** Próxima corrente nominal padrão estritamente acima de {@code correnteA}. */
    public OptionalInt proximoDisjuntorAcima(double correnteA) {
        return disjuntores.stream().filter(d -> d > correnteA).mapToInt(Integer::intValue).findFirst();
    }

    public List<Double> secoesComerciais(Isolante isolante) {
        return secoes.get(isolante);
    }

    /** Menor seção comercial ≥ {@code secaoMm2}. */
    public OptionalDouble menorSecaoComercialMaiorIgual(Isolante isolante, double secaoMm2) {
        return secoes.get(isolante).stream().filter(s -> s >= secaoMm2).mapToDouble(Double::doubleValue).findFirst();
    }

    /** Próxima seção comercial estritamente acima de {@code secaoMm2}. */
    public OptionalDouble proximaSecaoAcima(Isolante isolante, double secaoMm2) {
        return secoes.get(isolante).stream().filter(s -> s > secaoMm2).mapToDouble(Double::doubleValue).findFirst();
    }

    /**
     * Capacidade de condução (A) da seção {@code secaoMm2} no método/isolante dados
     * (Tabelas 36/38), para 2 ou 3 condutores carregados.
     */
    public OptionalDouble capacidadeConducao(Isolante isolante, MetodoInstalacao metodo,
                                             int condutoresCarregados, double secaoMm2) {
        Map<String, List<Double>> porCond = capacidades.get(isolante).get(metodo);
        if (porCond == null) {
            return OptionalDouble.empty();
        }
        List<Double> coluna = colunaCapacidade(porCond, condutoresCarregados);
        if (coluna == null) {
            return OptionalDouble.empty();
        }
        List<Double> lista = secoes.get(isolante);
        int idx = lista.indexOf(secaoMm2);
        if (idx < 0 || idx >= coluna.size() || coluna.get(idx) == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(coluna.get(idx));
    }

    private static List<Double> colunaCapacidade(Map<String, List<Double>> porCond, int condutores) {
        List<String> candidatas = condutores <= 2
                ? List.of("2", "3", "3TRI", "3trifolio", "3HOR", "3horizontal")
                : List.of("3", "3TRI", "3trifolio", "3HOR", "3horizontal");
        for (String chave : candidatas) {
            List<Double> coluna = porCond.get(chave);
            if (coluna != null) {
                return coluna;
            }
        }
        return null;
    }

    /** Seção do neutro (Tab. 48) para a seção fase dada. */
    public OptionalDouble neutroPara(double faseMm2) {
        return lookupExatoOuMenorIgualFase(neutro, faseMm2);
    }

    /** Seção do condutor de proteção/terra (Tab. 58) para a seção fase dada. */
    public OptionalDouble terraPara(double faseMm2) {
        return lookupExatoOuMenorIgualFase(terra, faseMm2);
    }

    private static OptionalDouble lookupExatoOuMenorIgualFase(TreeMap<Double, Double> tabela, double faseMm2) {
        Double exato = tabela.get(faseMm2);
        if (exato != null) {
            return OptionalDouble.of(exato);
        }
        // seções fase abaixo da menor linha tabelada: condutor igual à fase
        if (!tabela.isEmpty() && faseMm2 < tabela.firstKey()) {
            return OptionalDouble.of(faseMm2);
        }
        return OptionalDouble.empty();
    }

    // ── Carga ──────────────────────────────────────────────────────────────────

    public static TabelasNbr5410 carregarDoClasspath() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode agrupamento = ler(mapper, "nbr5410/agrupamento.json");
        JsonNode temperatura = ler(mapper, "nbr5410/temperatura.json");
        JsonNode disjuntoresJson = ler(mapper, "nbr5410/disjuntores.json");
        JsonNode cap70 = ler(mapper, "nbr5410/cap-conducao-70.json");
        JsonNode cap90 = ler(mapper, "nbr5410/cap-conducao-90.json");
        JsonNode neutroJson = ler(mapper, "nbr5410/neutro.json");
        JsonNode terraJson = ler(mapper, "nbr5410/terra.json");

        Map<Integer, FormaAgrupamento> formas = new LinkedHashMap<>();
        for (JsonNode forma : agrupamento.get("formas")) {
            List<FaixaAgrupamento> faixas = new ArrayList<>();
            for (JsonNode f : forma.get("fatores")) {
                Integer max = f.get("maxCircuitos").isNull() ? null : f.get("maxCircuitos").asInt();
                faixas.add(new FaixaAgrupamento(f.get("minCircuitos").asInt(), max, f.get("fator").asDouble()));
            }
            int ref = forma.get("ref").asInt();
            formas.put(ref, new FormaAgrupamento(ref, forma.get("descricao").asText(), List.copyOf(faixas)));
        }

        Map<Integer, double[]> tempAmbiente = lerTemperaturas(temperatura.get("ambiente"));
        Map<Integer, double[]> tempSolo = lerTemperaturas(temperatura.get("solo"));

        List<Integer> disjuntores = new ArrayList<>();
        disjuntoresJson.get("correntesNominaisPadrao").forEach(n -> disjuntores.add(n.asInt()));
        Collections.sort(disjuntores);

        Map<Isolante, List<Double>> secoes = new EnumMap<>(Isolante.class);
        Map<Isolante, Map<MetodoInstalacao, Map<String, List<Double>>>> capacidades = new EnumMap<>(Isolante.class);
        secoes.put(Isolante.PVC, lerSecoes(cap70));
        secoes.put(Isolante.EPR, lerSecoes(cap90));
        capacidades.put(Isolante.PVC, lerCapacidades(cap70));
        capacidades.put(Isolante.EPR, lerCapacidades(cap90));

        return new TabelasNbr5410(formas, tempAmbiente, tempSolo, disjuntores, secoes, capacidades,
                lerParesSecao(neutroJson, "neutroMm2"), lerParesSecao(terraJson, "terraMm2"));
    }

    private static JsonNode ler(ObjectMapper mapper, String recurso) {
        try (InputStream in = TabelasNbr5410.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new CalculoException("Tabela normativa não encontrada no classpath: " + recurso);
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new CalculoException("Falha ao ler tabela normativa " + recurso + ": " + e.getMessage());
        }
    }

    private static Map<Integer, double[]> lerTemperaturas(JsonNode linhas) {
        Map<Integer, double[]> mapa = new TreeMap<>();
        for (JsonNode linha : linhas) {
            double pvc = linha.hasNonNull("pvc") ? linha.get("pvc").asDouble() : Double.NaN;
            double epr = linha.hasNonNull("epr") ? linha.get("epr").asDouble() : Double.NaN;
            mapa.put(linha.get("temperatura").asInt(), new double[]{pvc, epr});
        }
        return mapa;
    }

    private static List<Double> lerSecoes(JsonNode cap) {
        List<Double> lista = new ArrayList<>();
        cap.get("secoesMm2").forEach(s -> lista.add(s.asDouble()));
        return List.copyOf(lista);
    }

    private static Map<MetodoInstalacao, Map<String, List<Double>>> lerCapacidades(JsonNode cap) {
        Map<MetodoInstalacao, Map<String, List<Double>>> porMetodo = new EnumMap<>(MetodoInstalacao.class);
        cap.get("capacidades").fields().forEachRemaining(metodoEntry -> {
            Optional<MetodoInstalacao> metodo = parseMetodo(metodoEntry.getKey());
            if (metodo.isEmpty()) {
                return;
            }
            Map<String, List<Double>> porCond = new LinkedHashMap<>();
            metodoEntry.getValue().fields().forEachRemaining(condEntry -> {
                List<Double> coluna = new ArrayList<>();
                condEntry.getValue().forEach(v -> coluna.add(v.isNull() ? null : v.asDouble()));
                porCond.put(condEntry.getKey(), Collections.unmodifiableList(coluna));
            });
            porMetodo.put(metodo.get(), Collections.unmodifiableMap(porCond));
        });
        return porMetodo;
    }

    private static Optional<MetodoInstalacao> parseMetodo(String chave) {
        try {
            return Optional.of(MetodoInstalacao.valueOf(chave));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static TreeMap<Double, Double> lerParesSecao(JsonNode json, String campoValor) {
        TreeMap<Double, Double> mapa = new TreeMap<>();
        for (JsonNode linha : json.get("linhas")) {
            mapa.put(linha.get("faseMm2").asDouble(), linha.get(campoValor).asDouble());
        }
        return mapa;
    }
}
