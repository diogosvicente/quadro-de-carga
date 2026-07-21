package br.uerj.eletrica.calc;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                MetodoInstalacao.B1, 2.0, 30, 7319, 7442, 40, 6.0);
        ResultadoAlimentador r = calculadora.calcular(in);

        assertEquals(19.5, r.correnteTotalA());
        assertEquals(25, r.correnteCalculadaA());
        assertEquals(50, r.disjuntorGeral().correnteA());
        assertEquals("3P 50A", r.disjuntorGeral().rotulo());
        assertEquals(10.0, r.secaoMinimaPorRegraMm2());
        assertEquals(10.0, r.secaoAlimentadorMm2());
        assertEquals(10.0, r.secaoNeutroMm2());
        assertEquals(10.0, r.secaoTerraMm2());
        assertEquals("3F#10mm²+N10mm²+T10mm²", r.rotuloCabo());
        assertTrue(r.quedaCalculadaPct() <= 2.0);
        // capacidade do quadro (planilha BP2): √3×220×50×0,8 = 15 242 VA
        assertEquals(15242, r.capacidadeQuadroVA());
    }

    @Test
    void quedaDeTensao_forcaSecaoMaiorEmAlimentadorLongo() {
        // Sem circuitos de referência (mínimos nulos): 200 m, 20 kVA, 3F/380.
        // I = 20000/(√3×380) = 30,4 A. Com seção pela capacidade, a queda em 200 m estoura 2%
        // e o cálculo deve subir a seção até atender.
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V220_380, 3, 1.0, 0, 200,
                MetodoInstalacao.B1, 2.0, 30, 20000, 20000, null, null);
        ResultadoAlimentador r = calculadora.calcular(in);

        assertEquals(30.4, r.correnteTotalA());
        assertTrue(r.secaoAlimentadorMm2() > r.secaoPorCapacidadeMm2(),
                "queda de tensão deve elevar a seção acima da exigida por capacidade");
        assertTrue(r.quedaCalculadaPct() <= 2.0);
    }

    @Test
    void fatorDeDemandaECargaReserva_entramNaCargaDemandada() {
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V127_220, 3, 0.8, 1000, 10,
                MetodoInstalacao.B1, 2.0, 30, 10000, 10000, null, null);
        ResultadoAlimentador r = calculadora.calcular(in);
        // (10000 + 1000) × 0,8 = 8800 VA
        assertEquals(8800, r.cargaDemandadaVA());
    }

    @Test
    void alimentadorMonofasico_usaTensaoFaseNeutro() {
        EntradaAlimentador in = new EntradaAlimentador(SistemaTensao.V127_220, 1, 1.0, 0, 10,
                MetodoInstalacao.B1, 2.0, 30, 2540, 2540, null, null);
        ResultadoAlimentador r = calculadora.calcular(in);
        assertEquals(127, r.tensaoV());
        assertEquals(20.0, r.correnteTotalA()); // 2540/127
    }
}
