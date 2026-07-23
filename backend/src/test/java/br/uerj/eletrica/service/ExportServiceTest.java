package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.CalculadoraAlimentador;
import br.uerj.eletrica.calc.CalculadoraCircuito;
import br.uerj.eletrica.calc.EntradaAlimentador;
import br.uerj.eletrica.calc.EntradaCircuito;
import br.uerj.eletrica.calc.ResultadoAlimentador;
import br.uerj.eletrica.calc.ResultadoCircuito;
import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;
import br.uerj.eletrica.domain.TipoCircuito;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;
import br.uerj.eletrica.dto.quadro.QuadroResponse;
import br.uerj.eletrica.dto.quadro.ResumoQuadroResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test da exportação (docs/CALCULOS.md §6c): QuadroService.resumo é stubado, então o teste
 * roda sem banco. Verifica que o XLSX é um OOXML (ZIP, começa com "PK") e que o DXF tem estrutura
 * mínima (SECTION/EOF) com a linha do alimentador e "mm²" escrito como "mm2".
 */
class ExportServiceTest {

    private static ResumoQuadroResponse resumoExemplo() {
        TabelasNbr5410 tabelas = TabelasNbr5410.carregarDoClasspath();

        ResultadoCircuito rc = new CalculadoraCircuito(tabelas).calcular(new EntradaCircuito(
                TipoCircuito.FORCA, 220, 1, 1419, 0.92, 19, 1, 30,
                MetodoInstalacao.B1, Isolante.PVC, 1, 1.0, 4.0, false, 1));
        CircuitoResponse circuito = new CircuitoResponse(
                1L, 10L, 2, "Freezers", TipoCircuito.FORCA, 220, 1,
                BigDecimal.valueOf(1419), BigDecimal.valueOf(0.92), BigDecimal.valueOf(19),
                1, 1, 30, MetodoInstalacao.B1, Isolante.PVC, 1,
                BigDecimal.ONE, BigDecimal.valueOf(4), false, rc);

        ResultadoAlimentador alimentador = new CalculadoraAlimentador(tabelas).calcular(new EntradaAlimentador(
                SistemaTensao.V127_220, 3, 1.0, 0, 20, MetodoInstalacao.B1, 2.0, 30,
                7442, 7442, 40, 16.0, 1));

        QuadroResponse quadro = new QuadroResponse(
                10L, "QDC Exemplo", "UERJ", SistemaTensao.V127_220, 3, 1,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(20), MetodoInstalacao.B1,
                BigDecimal.valueOf(2), 30, 1L, BigDecimal.valueOf(1419));

        return new ResumoQuadroResponse(quadro, alimentador, List.of(circuito));
    }

    private static ExportService comResumo(ResumoQuadroResponse resumo) {
        QuadroService quadroService = Mockito.mock(QuadroService.class);
        Mockito.when(quadroService.resumo(10L)).thenReturn(resumo);
        return new ExportService(quadroService);
    }

    @Test
    void xlsx_geraArquivoOoxml() {
        byte[] xlsx = comResumo(resumoExemplo()).xlsx(10L);
        assertTrue(xlsx.length > 2, "xlsx não pode ser vazio");
        // OOXML (.xlsx) é um contêiner ZIP → começa com a assinatura "PK"
        assertEquals("PK", new String(xlsx, 0, 2, StandardCharsets.US_ASCII));
    }

    @Test
    void dxf_geraTabelaR12ComAlimentador() {
        String dxf = new String(comResumo(resumoExemplo()).dxf(10L), StandardCharsets.ISO_8859_1);
        assertTrue(dxf.contains("SECTION"), "DXF deve conter SECTION");
        assertTrue(dxf.contains("EOF"), "DXF deve terminar com EOF");
        assertTrue(dxf.contains("Alimentador geral"), "DXF deve conter a linha do alimentador");
        // §6c: "mm²" é escrito como "mm2" no DXF por compatibilidade de codepage
        assertTrue(dxf.contains("mm2"), "seções no DXF usam 'mm2'");
        assertFalse(dxf.contains("mm²"), "DXF não deve conter o caractere '²'");
    }
}
