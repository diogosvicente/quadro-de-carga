package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.CalculadoraCircuito;
import br.uerj.eletrica.calc.EntradaCircuito;
import br.uerj.eletrica.calc.ResultadoCircuito;
import br.uerj.eletrica.domain.Circuito;
import br.uerj.eletrica.domain.Quadro;
import br.uerj.eletrica.dto.circuito.CircuitoRequest;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;
import br.uerj.eletrica.repository.CircuitoRepository;
import br.uerj.eletrica.repository.QuadroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CircuitoService {

    static final Set<Integer> TENSOES_VALIDAS = Set.of(127, 220, 380, 440, 660);

    private final CircuitoRepository circuitos;
    private final QuadroRepository quadros;
    private final CalculadoraCircuito calculadora;

    public CircuitoService(CircuitoRepository circuitos, QuadroRepository quadros,
                           CalculadoraCircuito calculadora) {
        this.circuitos = circuitos;
        this.quadros = quadros;
        this.calculadora = calculadora;
    }

    @Transactional(readOnly = true)
    public List<CircuitoResponse> listar(Long quadroId) {
        exigirQuadro(quadroId);
        return circuitos.findByQuadroIdOrderByNumero(quadroId).stream()
                .map(c -> CircuitoResponse.de(c, calcular(c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CircuitoResponse buscar(Long quadroId, Long circuitoId) {
        Circuito c = circuitos.findByIdAndQuadroId(circuitoId, quadroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Circuito não encontrado."));
        return CircuitoResponse.de(c, calcular(c));
    }

    @Transactional
    public CircuitoResponse criar(Long quadroId, CircuitoRequest request) {
        Quadro quadro = exigirQuadro(quadroId);
        CircuitoRequest req = validar(request);
        if (circuitos.existsByQuadroIdAndNumero(quadroId, req.numero())) {
            throw new RegraDeNegocioException("Já existe o circuito nº " + req.numero() + " neste quadro.");
        }
        Circuito c = new Circuito();
        c.setQuadro(quadro);
        aplicar(req, c);
        calcular(c); // valida o dimensionamento antes de persistir
        circuitos.save(c);
        return CircuitoResponse.de(c, calcular(c));
    }

    @Transactional
    public CircuitoResponse atualizar(Long quadroId, Long circuitoId, CircuitoRequest request) {
        Circuito c = circuitos.findByIdAndQuadroId(circuitoId, quadroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Circuito não encontrado."));
        CircuitoRequest req = validar(request);
        if (circuitos.existsByQuadroIdAndNumeroAndIdNot(quadroId, req.numero(), circuitoId)) {
            throw new RegraDeNegocioException("Já existe o circuito nº " + req.numero() + " neste quadro.");
        }
        aplicar(req, c);
        ResultadoCircuito resultado = calcular(c);
        return CircuitoResponse.de(c, resultado);
    }

    @Transactional
    public void remover(Long quadroId, Long circuitoId) {
        Circuito c = circuitos.findByIdAndQuadroId(circuitoId, quadroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Circuito não encontrado."));
        circuitos.delete(c);
    }

    /** Prévia sem persistir (POST /api/calculos/circuito). */
    public ResultadoCircuito calcularPrevia(CircuitoRequest request) {
        CircuitoRequest req = validar(request);
        return calculadora.calcular(new EntradaCircuito(
                req.tipo(), req.tensaoV(), req.fases(),
                req.potenciaW().doubleValue(), req.fatorPotencia().doubleValue(),
                req.comprimentoM().doubleValue(), req.circuitosAgrupados(), req.temperaturaC(),
                req.metodoInstalacao(), req.isolante(), req.formaAgrupamentoRef(),
                req.fatorDemanda().doubleValue(), req.quedaAdmissivelPct().doubleValue(),
                req.linhaSubterranea()));
    }

    public ResultadoCircuito calcular(Circuito c) {
        return calculadora.calcular(new EntradaCircuito(
                c.getTipo(), c.getTensaoV(), c.getFases(),
                c.getPotenciaW().doubleValue(), c.getFatorPotencia().doubleValue(),
                c.getComprimentoM().doubleValue(), c.getCircuitosAgrupados(), c.getTemperaturaC(),
                c.getMetodoInstalacao(), c.getIsolante(), c.getFormaAgrupamentoRef(),
                c.getFatorDemanda().doubleValue(), c.getQuedaAdmissivelPct().doubleValue(),
                c.getLinhaSubterranea()));
    }

    private CircuitoRequest validar(CircuitoRequest request) {
        CircuitoRequest req = request.comPadroes();
        if (!TENSOES_VALIDAS.contains(req.tensaoV())) {
            throw new RegraDeNegocioException("Tensão inválida: " + req.tensaoV()
                    + " V. Valores aceitos: 127, 220, 380, 440 ou 660 V.");
        }
        return req;
    }

    private void aplicar(CircuitoRequest req, Circuito c) {
        c.setNumero(req.numero());
        c.setDescricao(req.descricao());
        c.setTipo(req.tipo());
        c.setTensaoV(req.tensaoV());
        c.setFases(req.fases());
        c.setPotenciaW(req.potenciaW());
        c.setFatorPotencia(req.fatorPotencia());
        c.setComprimentoM(req.comprimentoM());
        c.setCircuitosAgrupados(req.circuitosAgrupados());
        c.setTemperaturaC(req.temperaturaC());
        c.setMetodoInstalacao(req.metodoInstalacao());
        c.setIsolante(req.isolante());
        c.setFormaAgrupamentoRef(req.formaAgrupamentoRef());
        c.setFatorDemanda(req.fatorDemanda());
        c.setQuedaAdmissivelPct(req.quedaAdmissivelPct());
        c.setLinhaSubterranea(req.linhaSubterranea());
    }

    private Quadro exigirQuadro(Long quadroId) {
        return quadros.findById(quadroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Quadro não encontrado."));
    }
}
