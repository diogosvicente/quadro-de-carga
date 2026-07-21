package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.CalculadoraAlimentador;
import br.uerj.eletrica.calc.EntradaAlimentador;
import br.uerj.eletrica.calc.ResultadoAlimentador;
import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.Circuito;
import br.uerj.eletrica.domain.Quadro;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;
import br.uerj.eletrica.dto.quadro.QuadroRequest;
import br.uerj.eletrica.dto.quadro.QuadroResponse;
import br.uerj.eletrica.dto.quadro.ResumoQuadroResponse;
import br.uerj.eletrica.repository.CircuitoRepository;
import br.uerj.eletrica.repository.QuadroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class QuadroService {

    private final QuadroRepository quadros;
    private final CircuitoRepository circuitos;
    private final CircuitoService circuitoService;
    private final CalculadoraAlimentador calculadoraAlimentador;
    private final TabelasNbr5410 tabelas;

    public QuadroService(QuadroRepository quadros, CircuitoRepository circuitos,
                         CircuitoService circuitoService, CalculadoraAlimentador calculadoraAlimentador,
                         TabelasNbr5410 tabelas) {
        this.quadros = quadros;
        this.circuitos = circuitos;
        this.circuitoService = circuitoService;
        this.calculadoraAlimentador = calculadoraAlimentador;
        this.tabelas = tabelas;
    }

    @Transactional(readOnly = true)
    public List<QuadroResponse> listar() {
        return quadros.findAll(org.springframework.data.domain.Sort.by("nome")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuadroResponse buscar(Long id) {
        return toResponse(exigir(id));
    }

    @Transactional
    public QuadroResponse criar(QuadroRequest req) {
        if (quadros.existsByNomeIgnoreCase(req.nome().trim())) {
            throw new RegraDeNegocioException("Já existe um quadro com o nome \"" + req.nome().trim() + "\".");
        }
        Quadro q = new Quadro();
        aplicar(req, q);
        quadros.save(q);
        return toResponse(q);
    }

    @Transactional
    public QuadroResponse atualizar(Long id, QuadroRequest req) {
        Quadro q = exigir(id);
        if (quadros.existsByNomeIgnoreCaseAndIdNot(req.nome().trim(), id)) {
            throw new RegraDeNegocioException("Já existe um quadro com o nome \"" + req.nome().trim() + "\".");
        }
        aplicar(req, q);
        return toResponse(q);
    }

    @Transactional
    public void remover(Long id) {
        quadros.delete(exigir(id));
    }

    /** Resumo do quadro: circuitos calculados + alimentador geral (docs/CALCULOS.md §3). */
    @Transactional(readOnly = true)
    public ResumoQuadroResponse resumo(Long id) {
        Quadro quadro = exigir(id);
        List<Circuito> lista = circuitos.findByQuadroIdOrderByNumero(id);
        List<CircuitoResponse> calculados = lista.stream()
                .map(c -> CircuitoResponse.de(c, circuitoService.calcular(c)))
                .toList();

        ResultadoAlimentador alimentador = null;
        if (!calculados.isEmpty()) {
            double somaW = calculados.stream().mapToDouble(c -> c.potenciaW().doubleValue()).sum();
            double somaVA = calculados.stream().mapToDouble(c -> c.resultado().potenciaVA()).sum();
            Integer maiorDisjuntor = calculados.stream()
                    .map(c -> c.resultado().disjuntor().correnteA())
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            Double maiorSecao = calculados.stream()
                    .map(c -> c.resultado().secaoFinalMm2())
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            alimentador = calculadoraAlimentador.calcular(new EntradaAlimentador(
                    quadro.getSistemaTensao(), quadro.getFasesAlimentador(),
                    quadro.getFatorDemanda().doubleValue(), quadro.getCargaReservaVA().doubleValue(),
                    quadro.getComprimentoM().doubleValue(), quadro.getMetodoInstalacao(),
                    quadro.getQuedaAdmissivelPct().doubleValue(), quadro.getTemperaturaC(),
                    somaW, somaVA, maiorDisjuntor, maiorSecao));
        }
        return new ResumoQuadroResponse(toResponse(quadro), alimentador, calculados);
    }

    private QuadroResponse toResponse(Quadro q) {
        long total = circuitos.countByQuadroId(q.getId());
        BigDecimal potenciaTotal = circuitos.findByQuadroIdOrderByNumero(q.getId()).stream()
                .map(Circuito::getPotenciaW)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return QuadroResponse.de(q, total, potenciaTotal);
    }

    private void aplicar(QuadroRequest req, Quadro q) {
        // o resumo calcula o alimentador em EPR com esta temperatura — validar na escrita
        // para não deixar o quadro num estado em que o GET /resumo falha (revisão adversarial)
        if (tabelas.fatorTemperatura(req.temperaturaC(), Isolante.EPR, false).isEmpty()) {
            throw new RegraDeNegocioException("Temperatura de " + req.temperaturaC()
                    + " °C sem fator de correção na NBR 5410. Temperaturas tabeladas: "
                    + tabelas.temperaturasTabeladas(Isolante.EPR, false) + ".");
        }
        q.setNome(req.nome().trim());
        q.setLocal(req.local());
        q.setSistemaTensao(req.sistemaTensao());
        q.setFasesAlimentador(req.fasesAlimentador());
        q.setFatorDemanda(req.fatorDemanda());
        q.setCargaReservaVA(req.cargaReservaVA());
        q.setComprimentoM(req.comprimentoM());
        q.setMetodoInstalacao(req.metodoInstalacao());
        q.setQuedaAdmissivelPct(req.quedaAdmissivelPct());
        q.setTemperaturaC(req.temperaturaC());
    }

    private Quadro exigir(Long id) {
        return quadros.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadro não encontrado."));
    }
}
