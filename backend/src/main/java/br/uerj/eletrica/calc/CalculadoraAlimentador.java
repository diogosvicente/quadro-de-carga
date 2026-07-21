package br.uerj.eletrica.calc;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;

import static br.uerj.eletrica.calc.CalculadoraCircuito.arredondar;
import static br.uerj.eletrica.calc.CalculadoraCircuito.formatarSecao;

/**
 * Dimensionamento do alimentador geral do quadro conforme NBR 5410.
 * Implementa docs/CALCULOS.md §3: alimentador em EPR/XLPE 90 °C, condutividade 56 m/(Ω·mm²),
 * com a regra do Rodrigo de "um valor padrão acima do maior circuito" como mínimo
 * para o disjuntor geral e para a seção do cabo.
 */
public final class CalculadoraAlimentador {

    static final double CONDUTIVIDADE_COBRE_ALIMENTADOR = 56.0; // m/(Ω·mm²) — planilha, alimentador (EPR)
    private static final double RAIZ_3 = Math.sqrt(3);
    private static final Isolante ISOLANTE_ALIMENTADOR = Isolante.EPR;
    private static final double SECAO_MINIMA_ALIMENTADOR_MM2 = 2.5;

    private final TabelasNbr5410 tabelas;

    public CalculadoraAlimentador(TabelasNbr5410 tabelas) {
        this.tabelas = tabelas;
    }

    public ResultadoAlimentador calcular(EntradaAlimentador in) {
        // §3.1 totais
        double cargaDemandadaVA = (in.somaPotenciaVA() + in.cargaReservaVA()) * in.fatorDemanda();

        // §3.2 corrente total
        int vfn = in.sistemaTensao().getFaseNeutroV();
        int vff = in.sistemaTensao().getFaseFaseV();
        int tensaoV = in.fases() == 1 ? vfn : vff;
        double divisor = in.fases() == 3 ? RAIZ_3 * vff : tensaoV;
        double correnteTotalA = arredondar(cargaDemandadaVA / divisor, 1);

        // §3.3 disjuntor geral — regra dos 80% da planilha (coluna B da aba DISJUNTORES):
        // menor In padrão cuja carga de uso contínuo (0,8×In) comporta a corrente total
        int inCalculado = tabelas.menorDisjuntorMaiorIgual(correnteTotalA / 0.8)
                .orElseThrow(() -> new CalculoException(
                        "Corrente total de " + correnteTotalA + " A acima do maior disjuntor da tabela."));
        int inGeral = inCalculado;
        if (in.maiorDisjuntorCircuitoA() != null) {
            int minimoPorRegra = tabelas.proximoDisjuntorAcima(in.maiorDisjuntorCircuitoA())
                    .orElseThrow(() -> new CalculoException(
                            "Não há disjuntor padrão acima de " + in.maiorDisjuntorCircuitoA() + " A."));
            inGeral = Math.max(inCalculado, minimoPorRegra);
        }
        ResultadoCircuito.Disjuntor disjuntorGeral = new ResultadoCircuito.Disjuntor(
                in.fases(), inGeral, in.fases() + "P " + inGeral + "A");

        // §3.4 cabo alimentador — capacidade de condução (90 °C), sem agrupamento (alimentador único)
        double fTemp = tabelas.fatorTemperatura(in.temperaturaC(), ISOLANTE_ALIMENTADOR, false)
                .orElseThrow(() -> new CalculoException(
                        "Temperatura de " + in.temperaturaC() + " °C sem fator de correção (EPR). Temperaturas tabeladas: "
                                + tabelas.temperaturasTabeladas(ISOLANTE_ALIMENTADOR, false) + "."));
        int condutoresCarregados = in.fases() == 3 ? 3 : 2;
        double secaoPorCapacidade = Math.max(SECAO_MINIMA_ALIMENTADOR_MM2,
                menorSecaoComIzCorrigida(in, condutoresCarregados, fTemp, inGeral));

        Double secaoMinimaPorRegra = null;
        if (in.maiorSecaoCircuitoMm2() != null) {
            secaoMinimaPorRegra = tabelas.proximaSecaoAcima(ISOLANTE_ALIMENTADOR, in.maiorSecaoCircuitoMm2())
                    .orElseThrow(() -> new CalculoException(
                            "Não há seção comercial acima de " + in.maiorSecaoCircuitoMm2() + " mm²."));
        }
        double secao = Math.max(secaoPorCapacidade, secaoMinimaPorRegra == null ? 0 : secaoMinimaPorRegra);

        // §3.4 verificação iterativa de queda de tensão (limite do quadro, padrão 2%)
        double fatorFase = in.fases() == 3 ? RAIZ_3 : 2.0;
        while (quedaPct(fatorFase, in.comprimentoM(), correnteTotalA, secao, tensaoV) > in.quedaAdmissivelPct()) {
            var proxima = tabelas.proximaSecaoAcima(ISOLANTE_ALIMENTADOR, secao);
            if (proxima.isEmpty()) {
                throw new CalculoException("Queda de tensão do alimentador acima de " + in.quedaAdmissivelPct()
                        + "% mesmo com a maior seção tabelada — reduza o comprimento ou divida a alimentação.");
            }
            secao = proxima.getAsDouble();
        }
        final double secaoFinal = secao;
        double quedaCalculadaPct = quedaPct(fatorFase, in.comprimentoM(), correnteTotalA, secaoFinal, tensaoV);

        double secaoNeutro = tabelas.neutroPara(secaoFinal)
                .orElseThrow(() -> new CalculoException("Sem seção de neutro tabelada para " + secaoFinal + " mm²."));
        double secaoTerra = tabelas.terraPara(secaoFinal)
                .orElseThrow(() -> new CalculoException("Sem seção de terra tabelada para " + secaoFinal + " mm²."));
        String rotuloCabo = in.fases() + "F#" + formatarSecao(secaoFinal) + "mm²+N" + formatarSecao(secaoNeutro)
                + "mm²+T" + formatarSecao(secaoTerra) + "mm²";

        // §3.5 capacidade do quadro (planilha BP2: 381 × In × 0,8 no sistema 127/220 trifásico)
        double capacidadeQuadroVA = (in.fases() == 3 ? RAIZ_3 * vff : tensaoV) * inGeral * 0.8;

        return new ResultadoAlimentador(
                in.somaPotenciaW(),
                arredondar(in.somaPotenciaVA(), 0),
                arredondar(cargaDemandadaVA, 0),
                correnteTotalA,
                tensaoV,
                in.fases(),
                disjuntorGeral,
                inCalculado,
                in.maiorDisjuntorCircuitoA(),
                secao,
                secaoPorCapacidade,
                secaoMinimaPorRegra,
                in.maiorSecaoCircuitoMm2(),
                secaoNeutro,
                secaoTerra,
                rotuloCabo,
                arredondar(quedaCalculadaPct, 2),
                arredondar(capacidadeQuadroVA, 0));
    }

    private double menorSecaoComIzCorrigida(EntradaAlimentador in, int condutoresCarregados,
                                            double fTemp, double correnteMinimaA) {
        for (double secao : tabelas.secoesComerciais(ISOLANTE_ALIMENTADOR)) {
            var iz = tabelas.capacidadeConducao(ISOLANTE_ALIMENTADOR, in.metodoInstalacao(), condutoresCarregados, secao);
            if (iz.isPresent() && iz.getAsDouble() * fTemp >= correnteMinimaA) {
                return secao;
            }
        }
        throw new CalculoException("Nenhuma seção da tabela de 90 °C (método " + in.metodoInstalacao()
                + ") atende " + correnteMinimaA + " A — divida a alimentação.");
    }

    private static double quedaPct(double fatorFase, double comprimentoM, double correnteA,
                                   double secaoMm2, int tensaoV) {
        return (fatorFase * comprimentoM * correnteA * 100)
                / (CONDUTIVIDADE_COBRE_ALIMENTADOR * secaoMm2 * tensaoV);
    }
}
