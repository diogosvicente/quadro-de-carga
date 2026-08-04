package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.CalculadoraCircuito;
import br.uerj.eletrica.calc.ResultadoCircuito;
import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.TipoCircuito;
import br.uerj.eletrica.dto.circuito.CircuitoRequest;
import br.uerj.eletrica.dto.circuito.EquipamentoRequest;
import br.uerj.eletrica.repository.CircuitoRepository;
import br.uerj.eletrica.repository.QuadroRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Derivação da potência total a partir dos equipamentos (docs/CALCULOS.md §1.1b) —
 * o SOMARPRODUTO da planilha do Rodrigo. O service é instanciado com repositórios
 * Mockito (sem banco), como em ExportServiceTest; a calculadora é a real, para o
 * teste de round-trip com o motor de cálculo.
 */
class CircuitoServiceTest {

    private static CircuitoService service() {
        return new CircuitoService(
                Mockito.mock(CircuitoRepository.class),
                Mockito.mock(QuadroRepository.class),
                new CalculadoraCircuito(TabelasNbr5410.carregarDoClasspath()));
    }

    /** Request mínimo válido; potenciaW e equipamentos variam por caso. */
    private static CircuitoRequest request(int tensaoV, int fases, String fatorPotencia,
                                           BigDecimal potenciaW, List<EquipamentoRequest> equipamentos) {
        return new CircuitoRequest(1, "Teste", TipoCircuito.FORCA, tensaoV, fases,
                potenciaW, new BigDecimal(fatorPotencia), BigDecimal.TEN, 1, 30,
                null, null, null, null, null, null, null, equipamentos).comPadroes();
    }

    private static EquipamentoRequest porPotencia(String nome, int quantidade, String potenciaW) {
        return new EquipamentoRequest(nome, quantidade, new BigDecimal(potenciaW), null);
    }

    private static EquipamentoRequest porCorrente(String nome, int quantidade, String correnteA) {
        return new EquipamentoRequest(nome, quantidade, null, new BigDecimal(correnteA));
    }

    @Test
    void potencia_somaProdutoQuantidadePorPotenciaUnitaria() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of(
                porPotencia("Lâmpada LED", 10, "100"),
                porPotencia("Chuveiro", 2, "500")));
        // Σ (100×10 + 500×2) = 2000.00
        assertEquals(new BigDecimal("2000.00"), service().derivarPotencia(req).potenciaW());
    }

    @Test
    void corrente_monofasico_pIgualVxIxFp() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of(
                porCorrente("Motor", 10, "1")));
        // I = 10 A → P = 127 × 10 × 0.92 = 1168.40
        assertEquals(new BigDecimal("1168.40"), service().derivarPotencia(req).potenciaW());
    }

    @Test
    void corrente_monofasico_roundTripDevolveIpDe10A() {
        // conferência: o motor de cálculo, a partir da potência derivada, reencontra os 10 A
        ResultadoCircuito resultado = service().calcularPrevia(
                request(127, 1, "0.92", null, List.of(porCorrente("Motor", 10, "1"))));
        assertEquals(10.0, resultado.correnteProjetoA(), 1e-9);
    }

    @Test
    void corrente_trifasico_incluiRaizDe3() {
        CircuitoRequest req = request(380, 3, "0.92", null, List.of(
                porCorrente("Bomba", 2, "50")));
        // I = 100 A → P = √3 × 380 × 100 × 0.92 = 60552.4962... → 60552.50 (2 casas, HALF_UP)
        assertEquals(new BigDecimal("60552.50"), service().derivarPotencia(req).potenciaW());
    }

    @Test
    void misturarPotenciaECorrente_regraDeNegocio() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of(
                porPotencia("Lâmpada", 1, "100"),
                porCorrente("Motor", 1, "1")));
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> service().derivarPotencia(req));
        assertEquals("Não misture equipamentos por potência e por corrente no mesmo circuito.",
                ex.getMessage());
    }

    @Test
    void linhaComPotenciaECorrente_regraDeNegocio() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of(
                new EquipamentoRequest("Ambos", 1, new BigDecimal("100"), new BigDecimal("1"))));
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> service().derivarPotencia(req));
        assertEquals("Cada equipamento deve ter potência OU corrente.", ex.getMessage());
    }

    @Test
    void linhaSemPotenciaNemCorrente_regraDeNegocio() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of(
                new EquipamentoRequest("Nenhum", 1, null, null)));
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> service().derivarPotencia(req));
        assertEquals("Cada equipamento deve ter potência OU corrente.", ex.getMessage());
    }

    @Test
    void listaVaziaSemPotenciaTotal_regraDeNegocio() {
        CircuitoRequest req = request(127, 1, "0.92", null, List.of());
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class,
                () -> service().derivarPotencia(req));
        assertEquals("Informe a potência total (ou a corrente) do circuito, ou detalhe os equipamentos.",
                ex.getMessage());
    }

    @Test
    void listaNulaComPotenciaTotal_mantemComportamentoAntigo() {
        CircuitoRequest req = request(127, 1, "0.92", new BigDecimal("1419"), null);
        // sem equipamentos, a potência digitada segue intacta
        assertEquals(new BigDecimal("1419"), service().derivarPotencia(req).potenciaW());
    }
}
