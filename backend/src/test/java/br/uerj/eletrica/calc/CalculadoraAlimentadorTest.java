package br.uerj.eletrica.calc;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraAlimentadorTest {

    private static CalculadoraAlimentador calculadora;

    @BeforeAll
    static void carregar() {
        calculadora = new CalculadoraAlimentador(TabelasNbr5410.carregarDoClasspath());
    }

    @Test
    void regraDoRodrigo_disjuntorECaboUmPassoAcimaDoMaiorCircuito() {
        // Quadro do exemplo (Glide): soma 7319 W / 7442 VA, maior circuito com disjuntor 40 A e cabo 6 mm².
        // Corrente total 3F/220: 7442/(√3×220) = 19,5 A → disjuntor pela regra dos 80% = 25 A
        // (0,8×25 = 20 ≥ 19,5), mas o mínimo é o próximo padrão acima de 40 A → geral 50 A.
        // Cabo: próximo acima de 6 → 10 mm².
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V127_220, 3, 1.0, 0, 20,
                MetodoInstalacao.B1, 2.0, 30, 7319, 7442, 40, 6.0, 1);
        ResultadoAlimentador r = calculadora.calcular(in);

        assertEquals(19.5, r.correnteTotalA());
        assertEquals(25, r.correnteCalculadaA());
        assertEquals(50, r.disjuntorGeral().correnteA());
        assertEquals("3P 50A", r.disjuntorGeral().rotulo());
        assertEquals(10.0, r.secaoMinimaPorRegraMm2());
        assertEquals(10.0, r.secaoAlimentadorMm2());
        assertEquals(10.0, r.secaoNeutroMm2());
        assertEquals(10.0, r.secaoTerraMm2());
        // §6b P=1 (padrão): rótulo novo com contadores por condutor
        assertEquals(1, r.circuitosParalelos());
        assertEquals(10.0, r.secaoFaseParaleloMm2());
        assertEquals("3F#10mm² + 1N10mm² + 1T10mm²", r.rotuloCabo());
        assertTrue(r.quedaCalculadaPct() <= 2.0);
        // capacidade do quadro (planilha BP2): √3×220×50×0,8 = 15 242 VA
        assertEquals(15242, r.capacidadeQuadroVA());
    }

    /**
     * Alimentador trifásico com seção final 25 mm² (neutro 25, terra 16) — base dos casos dourados
     * de condutores em paralelo (§6b). Maior circuito 16 mm² e disjuntor 40 A forçam, pela regra do
     * Rodrigo, cabo 25 mm² (próximo acima de 16) e disjuntor geral 50 A.
     */
    private static EntradaAlimentador exemplo25(int circuitosParalelos) {
        return new EntradaAlimentador(SistemaTensao.V127_220, 3, 1.0, 0, 20,
                MetodoInstalacao.B1, 2.0, 30, 7442, 7442, 40, 16.0, circuitosParalelos);
    }

    @Test
    void alimentador25_paralelo1_rotuloNovoFormato() {
        ResultadoAlimentador r = calculadora.calcular(exemplo25(1));
        assertEquals(25.0, r.secaoAlimentadorMm2());
        assertEquals(25.0, r.secaoNeutroMm2());
        assertEquals(16.0, r.secaoTerraMm2());
        assertEquals("3P 50A", r.disjuntorGeral().rotulo());
        assertEquals(1, r.circuitosParalelos());
        assertEquals(25.0, r.secaoFaseParaleloMm2());
        assertEquals("3F#25mm² + 1N25mm² + 1T16mm²", r.rotuloCabo());
    }

    @Test
    void alimentador25_paralelo2_cadaViaCarregaMetadeDaCorrente() {
        ResultadoAlimentador r = calculadora.calcular(exemplo25(2));
        // disjuntor geral e corrente total continuam do circuito inteiro (não dividem por P)
        assertEquals("3P 50A", r.disjuntorGeral().rotulo());
        // capacidade por via usa In_geral/2 = 25 A (bastaria 2,5 mm²), mas a regra "um passo acima
        // do maior circuito (16 mm²)" = 25 mm² aplica-se à seção da via → vFase 25 mm².
        assertEquals(25.0, r.secaoAlimentadorMm2());
        assertEquals(2, r.circuitosParalelos());
        assertEquals(25.0, r.secaoFaseParaleloMm2());
        // 2×3 = 6 condutores de fase; neutro/terra da própria via (25 mm² → N25, T16)
        assertEquals(25.0, r.secaoNeutroMm2());
        assertEquals(16.0, r.secaoTerraMm2());
        assertEquals("6F#25mm² + 2N25mm² + 2T16mm²", r.rotuloCabo());
    }

    @Test
    void alimentadorGrande_p1DividaAAlimentacao_masP2Dimensiona() {
        // §6b (alimentador): quadro grande demais para um cabo só. 3F/220, ~460 kVA →
        // I_total ≈ 1207 A → disjuntor geral 1600 A (do circuito inteiro, não divide por P).
        // P=1: nenhuma seção EPR conduz 1600 A (máx. B1/EPR/3c = 1173 A) → CalculoException.
        EntradaAlimentador p1 = new EntradaAlimentador(SistemaTensao.V127_220, 3, 1.0, 0, 10,
                MetodoInstalacao.B1, 2.0, 30, 460000, 460000, null, null, 1);
        CalculoException e = assertThrows(CalculoException.class, () -> calculadora.calcular(p1));
        assertTrue(e.getMessage().contains("divida a alimentação"), e.getMessage());

        // P=2: cada via carrega In_geral/2 = 800 A → dimensiona sem erro; 2×3 = 6 condutores de fase.
        EntradaAlimentador p2 = new EntradaAlimentador(SistemaTensao.V127_220, 3, 1.0, 0, 10,
                MetodoInstalacao.B1, 2.0, 30, 460000, 460000, null, null, 2);
        ResultadoAlimentador r = calculadora.calcular(p2);
        assertEquals(1600, r.disjuntorGeral().correnteA());
        assertEquals(2, r.circuitosParalelos());
        assertTrue(r.rotuloCabo().startsWith("6F#"), r.rotuloCabo());
    }

    @Test
    void quedaDeTensao_forcaSecaoMaiorEmAlimentadorLongo() {
        // Sem circuitos de referência (mínimos nulos): 200 m, 20 kVA, 3F/380.
        // I = 20000/(√3×380) = 30,4 A. Com seção pela capacidade, a queda em 200 m estoura 2%
        // e o cálculo deve subir a seção até atender.
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V220_380, 3, 1.0, 0, 200,
                MetodoInstalacao.B1, 2.0, 30, 20000, 20000, null, null, 1);
        ResultadoAlimentador r = calculadora.calcular(in);

        assertEquals(30.4, r.correnteTotalA());
        assertTrue(r.secaoAlimentadorMm2() > r.secaoPorCapacidadeMm2(),
                "queda de tensão deve elevar a seção acima da exigida por capacidade");
        assertTrue(r.quedaCalculadaPct() <= 2.0);
    }

    @Test
    void fatorDeDemandaECargaReserva_entramNaCargaDemandada() {
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V127_220, 3, 0.8, 1000, 10,
                MetodoInstalacao.B1, 2.0, 30, 10000, 10000, null, null, 1);
        ResultadoAlimentador r = calculadora.calcular(in);
        // (10000 + 1000) × 0,8 = 8800 VA
        assertEquals(8800, r.cargaDemandadaVA());
    }

    @Test
    void alimentadorMonofasico_usaTensaoFaseNeutro() {
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V127_220, 1, 1.0, 0, 10,
                MetodoInstalacao.B1, 2.0, 30, 2540, 2540, null, null, 1);
        ResultadoAlimentador r = calculadora.calcular(in);
        assertEquals(127, r.tensaoV());
        assertEquals(20.0, r.correnteTotalA()); // 2540/127
    }
}
