package br.uerj.eletrica.calc.tabelas;

import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabelasNbr5410Test {

    private static TabelasNbr5410 tabelas;

    @BeforeAll
    static void carregar() {
        tabelas = TabelasNbr5410.carregarDoClasspath();
    }

    @Test
    void fatoresDeAgrupamento_tabela42() {
        assertEquals(1.0, tabelas.fatorAgrupamento(1, 1).orElseThrow());
        assertEquals(0.8, tabelas.fatorAgrupamento(1, 2).orElseThrow());
        assertEquals(0.5, tabelas.fatorAgrupamento(1, 10).orElseThrow()); // faixa 9 a 11
        assertEquals(0.38, tabelas.fatorAgrupamento(1, 25).orElseThrow()); // ≥ 20
        assertEquals(0.95, tabelas.fatorAgrupamento(3, 1).orElseThrow()); // camada única no teto
    }

    @Test
    void fatoresDeTemperatura_tabela40() {
        assertEquals(1.0, tabelas.fatorTemperatura(30, Isolante.PVC, false).orElseThrow());
        assertEquals(0.87, tabelas.fatorTemperatura(40, Isolante.PVC, false).orElseThrow());
        assertEquals(0.91, tabelas.fatorTemperatura(40, Isolante.EPR, false).orElseThrow());
        assertTrue(tabelas.fatorTemperatura(32, Isolante.PVC, false).isEmpty(), "temperatura não tabelada");
        assertEquals(1.0, tabelas.fatorTemperatura(20, Isolante.PVC, true).orElseThrow()); // solo
    }

    @Test
    void disjuntores_padroesComerciais() {
        assertEquals(10, tabelas.menorDisjuntorMaiorIgual(7).orElseThrow());
        assertEquals(40, tabelas.menorDisjuntorMaiorIgual(38.5).orElseThrow());
        assertEquals(40, tabelas.menorDisjuntorMaiorIgual(40).orElseThrow());
        assertEquals(50, tabelas.proximoDisjuntorAcima(40).orElseThrow());
    }

    @Test
    void capacidadesDeConducao_incluindoCorrecoesDeDigitacaoDaPlanilha() {
        // Valores da NBR 5410 Tab. 36 (B1, 2 condutores carregados)
        assertEquals(57, tabelas.capacidadeConducao(Isolante.PVC, MetodoInstalacao.B1, 2, 10).orElseThrow());
        // Erros de digitação da planilha corrigidos na extração (docs/dados-normativos):
        assertEquals(111, tabelas.capacidadeConducao(Isolante.PVC, MetodoInstalacao.B2, 2, 35).orElseThrow());
        assertEquals(183, tabelas.capacidadeConducao(Isolante.PVC, MetodoInstalacao.D, 2, 70).orElseThrow());
        assertEquals(749, tabelas.capacidadeConducao(Isolante.PVC, MetodoInstalacao.B1, 2, 630).orElseThrow());
    }

    @Test
    void secoesComerciais_arredondamentoParaCima() {
        assertEquals(6, tabelas.menorSecaoComercialMaiorIgual(Isolante.PVC, 5.34).orElseThrow());
        assertEquals(0.75, tabelas.menorSecaoComercialMaiorIgual(Isolante.PVC, 0.52).orElseThrow());
        assertEquals(10, tabelas.proximaSecaoAcima(Isolante.EPR, 6).orElseThrow());
        assertEquals(4, tabelas.proximaSecaoAcima(Isolante.EPR, 2.5).orElseThrow());
    }

    @Test
    void neutroETerra_tabelas48e58() {
        assertEquals(2.5, tabelas.neutroPara(2.5).orElseThrow());
        assertEquals(25, tabelas.neutroPara(50).orElseThrow());
        assertEquals(16, tabelas.terraPara(25).orElseThrow());
        assertEquals(16, tabelas.terraPara(35).orElseThrow());
        assertEquals(25, tabelas.terraPara(50).orElseThrow());
        // correção aplicada sobre a planilha: 150 mm² → PE 95 (S/2 = 75 → próxima normalizada)
        assertEquals(95, tabelas.terraPara(150).orElseThrow());
    }
}
