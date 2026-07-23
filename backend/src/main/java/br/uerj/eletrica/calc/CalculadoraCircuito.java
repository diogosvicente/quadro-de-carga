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
        // §6b P vias em paralelo por fase; cada via carrega In/P e Ic/P (disjuntor é do circuito inteiro).
        int p = Math.max(1, in.circuitosParalelos());

        // §2.1 potência aparente (planilha arredonda VA para inteiro antes da corrente)
        double potenciaVA = Math.round(in.potenciaW() / in.fatorPotencia());

        // §2.2 corrente de projeto (circuito inteiro)
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

        // §2.5 disjuntor — protege o circuito INTEIRO (In ≥ Ic; NÃO divide por P)
        int inDisjuntor = tabelas.menorDisjuntorMaiorIgual(ic)
                .orElseThrow(() -> new CalculoException(
                        "Corrente corrigida de " + arredondar(ic, 1)
                                + " A acima do maior disjuntor da tabela — divida o circuito."));
        ResultadoCircuito.Disjuntor disjuntor = new ResultadoCircuito.Disjuntor(
                in.fases(), inDisjuntor, in.fases() + "P " + inDisjuntor + "A");

        // §6b dimensionamento de CADA via (I/P): P vias menores dividem a carga.
        int condutoresCarregados = in.fases() == 3 ? 3 : 2;

        // §2.6/§6b sobrecorrente por via: menor seção com Iz' = Iz × fTotal ≥ In/P
        double vSobrecorrente = menorSecaoComIzCorrigida(in, condutoresCarregados, fTotal, (double) inDisjuntor / p);

        // §2.7/§6b queda por via: seção de queda cheia ÷ P (Ic/P na via)
        double fatorFase = in.fases() == 3 ? RAIZ_3 : 2.0;
        double vQuedaCalculada = (fatorFase * in.comprimentoM() * ic * 100)
                / (CONDUTIVIDADE_COBRE_CIRCUITOS * in.quedaAdmissivelPct() * in.tensaoV() * p);
        double vQueda = tabelas.menorSecaoComercialMaiorIgual(in.isolante(), vQuedaCalculada)
                .orElseThrow(() -> new CalculoException(
                        "Queda de tensão exige seção acima da maior tabelada — divida o circuito ou reduza o comprimento."));

        // §2.8/§6b seção mínima normativa (Tab. 47) — por condutor, NÃO divide
        double vMinima = in.tipo().getSecaoMinimaMm2();

        // §2.9/§6b seção de cada via (fase), neutro e terra a partir da fase da própria via
        double vFase = Math.max(vSobrecorrente, Math.max(vQueda, vMinima));
        double vNeutro = tabelas.neutroPara(vFase)
                .orElseThrow(() -> new CalculoException("Sem seção de neutro tabelada para fase de " + vFase + " mm²."));
        double vTerra = tabelas.terraPara(vFase)
                .orElseThrow(() -> new CalculoException("Sem seção de terra tabelada para fase de " + vFase + " mm²."));

        String rotulo = CabosParalelos.rotulo(in.fases(), p, vFase, vNeutro, vTerra);

        // queda efetiva do circuito com a seção equivalente P × vFase (informativo)
        double quedaCalculadaPct = (fatorFase * in.comprimentoM() * ic * 100)
                / (CONDUTIVIDADE_COBRE_CIRCUITOS * vFase * in.tensaoV() * p);

        return new ResultadoCircuito(
                potenciaVA,
                ip,
                arredondar(ic, 1),
                fAgrup,
                fTemp,
                arredondar(fTotal, 3),
                disjuntor,
                vSobrecorrente,
                arredondar(vQuedaCalculada, 2),
                vQueda,
                vMinima,
                vFase,
                vNeutro,
                vTerra,
                rotulo,
                arredondar(quedaCalculadaPct, 2),
                p,
                vFase);
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
