package br.uerj.eletrica.calc;

import static br.uerj.eletrica.calc.CalculadoraCircuito.formatarSecao;

/**
 * Formatação do rótulo do cabo com P vias em paralelo (docs/CALCULOS.md §6b).
 *
 * <p>Cada uma das P vias é um conjunto completo (fase + neutro + terra) já dimensionado para
 * {@code I/P} pelas calculadoras (decisão do Rodrigo, 22/07/2026): P vias menores dividem a carga,
 * de modo que um circuito grande demais para um cabo só passa a ser dimensionável. Um único
 * disjuntor protege o circuito inteiro (In para a corrente total; não divide por P).
 *
 * <p>Esta classe apenas formata o rótulo pt-BR
 * {@code {P×pólos}F#{vFase}mm² + {P}N{vNeutro}mm² + {P}T{vTerra}mm²}; todo o dimensionamento por via
 * fica nas calculadoras, que têm In/Ic/fTotal/método/L/e%/V.
 */
final class CabosParalelos {

    private CabosParalelos() {
    }

    /**
     * @param polos   nº de condutores de fase por via (fases do circuito/alimentador: 1, 2 ou 3)
     * @param p       nº de vias em paralelo (≥ 1)
     * @param vFase   seção comercial de cada condutor de fase da via (vFase de §6b)
     * @param vNeutro seção do neutro da via (Tab. 48 sobre vFase)
     * @param vTerra  seção do terra da via (Tab. 58 sobre vFase)
     */
    static String rotulo(int polos, int p, double vFase, double vNeutro, double vTerra) {
        return (p * polos) + "F#" + formatarSecao(vFase) + "mm² + "
                + p + "N" + formatarSecao(vNeutro) + "mm² + "
                + p + "T" + formatarSecao(vTerra) + "mm²";
    }
}
