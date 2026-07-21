package br.uerj.eletrica.calc;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.TipoCircuito;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valores dourados: circuito "Freezers" e replicação da linha 5 da planilha
 * (QUADRO DE CARGA_teste_queda de tensão_rev12.xlsx) — ver docs/CALCULOS.md.
 */
class CalculadoraCircuitoTest {

    private static TabelasNbr5410 tabelas;
    private static CalculadoraCircuito calculadora;

    @BeforeAll
    static void carregar() {
        tabelas = TabelasNbr5410.carregarDoClasspath();
        calculadora = new CalculadoraCircuito(tabelas);
    }

    private static EntradaCircuito freezers() {
        // Circuito real cadastrado pelo Rodrigo no Glide (telas do WhatsApp de 21/07/2026)
        return new EntradaCircuito(TipoCircuito.FORCA, 220, 1, 1419, 0.92, 19, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
    }

    @Test
    void freezers_correnteDeProjetoECorrigida() {
        ResultadoCircuito r = calculadora.calcular(freezers());
        assertEquals(1542, r.potenciaVA());
        assertEquals(7.0, r.correnteProjetoA());
        assertEquals(7.0, r.correnteCorrigidaA());
        assertEquals(1.0, r.fatorAgrupamento());
        assertEquals(1.0, r.fatorTemperatura());
    }

    @Test
    void freezers_disjuntorESecoes() {
        ResultadoCircuito r = calculadora.calcular(freezers());
        assertEquals(10, r.disjuntor().correnteA());
        assertEquals("1P 10A", r.disjuntor().rotulo());
        // queda: (2×19×7×100)/(58×4×220) = 0,52 mm² -> comercial 0,75 (bate com a tela do Glide)
        assertEquals(0.52, r.secaoQuedaCalculadaMm2());
        assertEquals(0.75, r.secaoQuedaMm2());
        // mínima normativa de força (Tab. 47) decide a seção final
        assertEquals(2.5, r.secaoMinimaMm2());
        assertEquals(2.5, r.secaoFinalMm2());
        assertEquals(2.5, r.secaoNeutroMm2());
        assertEquals(2.5, r.secaoTerraMm2());
        assertEquals("1F#2,5mm²+N2,5mm²+T2,5mm²", r.rotuloCabo());
    }

    @Test
    void quadroDeComando_fatoresDeCorrecaoElevamDisjuntorESecao() {
        // 5900 W, FP 1, 220 V/1F, 2 circuitos agrupados, 40 °C: Ic = 26,8/(0,8×0,87) = 38,5 A (tela do Glide)
        EntradaCircuito in = new EntradaCircuito(TipoCircuito.FORCA, 220, 1, 5900, 1.0, 25, 2, 40,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        ResultadoCircuito r = calculadora.calcular(in);
        assertEquals(26.8, r.correnteProjetoA());
        assertEquals(0.8, r.fatorAgrupamento());
        assertEquals(0.87, r.fatorTemperatura());
        assertEquals(38.5, r.correnteCorrigidaA());
        assertEquals(40, r.disjuntor().correnteA());
        // coordenação In ≤ Iz': 10 mm² (57 A × 0,696 = 39,7) não atende 40 A; 16 mm² atende
        assertEquals(16, r.secaoSobrecorrenteMm2());
        assertEquals(16, r.secaoFinalMm2());
        assertEquals(16, r.secaoTerraMm2());
    }

    @Test
    void linha5DaPlanilha_valoresCacheadosBatem() {
        // Linha 5: 10 000 VA, FP 1, 127 V/1F, L=10 m, queda 4% — células BF5=78,7, BI5=80, BU5=5,342..., BQ5=6
        EntradaCircuito in = new EntradaCircuito(TipoCircuito.FORCA, 127, 1, 10000, 1.0, 10, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        ResultadoCircuito r = calculadora.calcular(in);
        assertEquals(78.7, r.correnteProjetoA());
        assertEquals(78.7, r.correnteCorrigidaA());
        assertEquals(80, r.disjuntor().correnteA());
        assertEquals(5.34, r.secaoQuedaCalculadaMm2());
        assertEquals(6, r.secaoQuedaMm2());
    }

    @Test
    void trifasico_usaRaizDe3() {
        // 10 000 W, FP 1, 380 V/3F: Ip = 10000/(√3×380) = 15,2 A
        EntradaCircuito in = new EntradaCircuito(TipoCircuito.FORCA, 380, 3, 10000, 1.0, 30, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        ResultadoCircuito r = calculadora.calcular(in);
        assertEquals(15.2, r.correnteProjetoA());
        assertEquals(16, r.disjuntor().correnteA());
        assertEquals("3P 16A", r.disjuntor().rotulo());
    }

    @Test
    void iluminacaoESinalizacao_secoesMinimasDaTabela47() {
        EntradaCircuito ilum = new EntradaCircuito(TipoCircuito.ILUMINACAO, 127, 1, 300, 1.0, 5, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        assertEquals(1.5, calculadora.calcular(ilum).secaoFinalMm2());

        EntradaCircuito sinal = new EntradaCircuito(TipoCircuito.SINALIZACAO, 127, 1, 100, 1.0, 5, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        assertEquals(0.5, calculadora.calcular(sinal).secaoFinalMm2());
    }

    @Test
    void temperaturaForaDaTabela_erroClaro() {
        EntradaCircuito in = new EntradaCircuito(TipoCircuito.FORCA, 220, 1, 1000, 1.0, 10, 1, 32,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false);
        CalculoException e = assertThrows(CalculoException.class, () -> calculadora.calcular(in));
        assertTrue(e.getMessage().contains("32"));
    }
}
