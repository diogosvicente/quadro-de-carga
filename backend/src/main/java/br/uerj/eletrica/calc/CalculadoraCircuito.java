package br.uerj.eletrica.calc;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;

import java.util.List;

/**
 * Dimensionamento de circuito terminal conforme NBR 5410.
 * Implementa docs/CALCULOS.md §2 — qualquer mudança de fórmula deve atualizar aquele documento.
 */
public final class CalculadoraCircuito {

    static final double CONDUTIVIDADE_COBRE_CIRCUITOS = 58.0; // m/(Ω·mm²) — planilha, circuitos (PVC)
    private static final double RAIZ_3 = Math.sqrt(3);

    private final TabelasNbr5410 tabelas;

    public CalculadoraCircuito(TabelasNbr5410 tabelas) {
        this.tabelas = tabelas;
    }

    public ResultadoCircuito calcular(EntradaCircuito in) {
        // §2.1 potência aparente (planilha arredonda VA para inteiro antes da corrente)
        double potenciaVA = Math.round(in.potenciaW() / in.fatorPotencia());

        // §2.2 corrente de projeto
        double divisor = in.fases() == 3 ? RAIZ_3 * in.tensaoV() : in.tensaoV();
        double ip = arredondar(potenciaVA / divisor, 1);

        // §2.3 fatores de correção
        double fAgrup = tabelas.fatorAgrupamento(in.formaAgrupamentoRef(), in.circuitosAgrupados())
                .orElseThrow(() -> new CalculoException(
                        "Sem fator de agrupamento para a forma " + in.formaAgrupamentoRef()
                                + " com " + in.circuitosAgrupados() + " circuito(s)."));
        double fTemp = tabelas.fatorTemperatura(in.temperaturaC(), in.isolante(), in.linhaSubterranea())
                .orElseThrow(() -> new CalculoException(
                        "Temperatura de " + in.temperaturaC() + " °C sem fator de correção na NBR 5410 para "
                                + in.isolante().getRotulo() + ". Temperaturas tabeladas: "
                                + tabelas.temperaturasTabeladas(in.isolante(), in.linhaSubterranea()) + "."));
        double fTotal = fAgrup * fTemp;

        // §2.4 corrente corrigida
        double ic = (ip * in.fatorDemanda()) / fTotal;

        // §2.5 disjuntor
        int inDisjuntor = tabelas.menorDisjuntorMaiorIgual(ic)
                .orElseThrow(() -> new CalculoException(
                        "Corrente corrigida de " + arredondar(ic, 1)
                                + " A acima do maior disjuntor da tabela — divida o circuito."));
        ResultadoCircuito.Disjuntor disjuntor = new ResultadoCircuito.Disjuntor(
                in.fases(), inDisjuntor, in.fases() + "P " + inDisjuntor + "A");

        // §2.6 seção por sobrecorrente: menor seção com Iz' = Iz × fTotal ≥ In
        int condutoresCarregados = in.fases() == 3 ? 3 : 2;
        double sSobrecorrente = menorSecaoComIzCorrigida(in, condutoresCarregados, fTotal, inDisjuntor);

        // §2.7 seção por queda de tensão
        double fatorFase = in.fases() == 3 ? RAIZ_3 : 2.0;
        double sQuedaCalculada = (fatorFase * in.comprimentoM() * ic * 100)
                / (CONDUTIVIDADE_COBRE_CIRCUITOS * in.quedaAdmissivelPct() * in.tensaoV());
        double sQueda = tabelas.menorSecaoComercialMaiorIgual(in.isolante(), sQuedaCalculada)
                .orElseThrow(() -> new CalculoException(
                        "Queda de tensão exige seção acima da maior tabelada — divida o circuito ou reduza o comprimento."));

        // §2.8–2.9 seção final, neutro, terra
        double sMinima = in.tipo().getSecaoMinimaMm2();
        double sFinal = Math.max(sSobrecorrente, Math.max(sQueda, sMinima));
        double sNeutro = tabelas.neutroPara(sFinal)
                .orElseThrow(() -> new CalculoException("Sem seção de neutro tabelada para fase de " + sFinal + " mm²."));
        double sTerra = tabelas.terraPara(sFinal)
                .orElseThrow(() -> new CalculoException("Sem seção de terra tabelada para fase de " + sFinal + " mm²."));

        String rotuloCabo = in.fases() + "F#" + formatarSecao(sFinal) + "mm²+N" + formatarSecao(sNeutro)
                + "mm²+T" + formatarSecao(sTerra) + "mm²";

        double quedaCalculadaPct = (fatorFase * in.comprimentoM() * ic * 100)
                / (CONDUTIVIDADE_COBRE_CIRCUITOS * sFinal * in.tensaoV());

        return new ResultadoCircuito(
                potenciaVA,
                ip,
                arredondar(ic, 1),
                fAgrup,
                fTemp,
                arredondar(fTotal, 3),
                disjuntor,
                sSobrecorrente,
                arredondar(sQuedaCalculada, 2),
                sQueda,
                sMinima,
                sFinal,
                sNeutro,
                sTerra,
                rotuloCabo,
                arredondar(quedaCalculadaPct, 2));
    }

    private double menorSecaoComIzCorrigida(EntradaCircuito in, int condutoresCarregados,
                                            double fTotal, double correnteMinimaA) {
        List<Double> lista = tabelas.secoesComerciais(in.isolante());
        for (double secao : lista) {
            var iz = tabelas.capacidadeConducao(in.isolante(), in.metodoInstalacao(), condutoresCarregados, secao);
            if (iz.isPresent() && iz.getAsDouble() * fTotal >= correnteMinimaA) {
                return secao;
            }
        }
        throw new CalculoException("Nenhuma seção da tabela de capacidade (" + in.isolante().getRotulo()
                + ", método " + in.metodoInstalacao() + ") atende " + correnteMinimaA + " A — divida o circuito.");
    }

    static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }

    static String formatarSecao(double secaoMm2) {
        String texto = secaoMm2 == Math.floor(secaoMm2)
                ? String.valueOf((long) secaoMm2)
                : String.valueOf(secaoMm2);
        return texto.replace('.', ',');
    }
}
