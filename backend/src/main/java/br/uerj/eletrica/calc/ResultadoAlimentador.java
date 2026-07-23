package br.uerj.eletrica.calc;

/** Resultado do dimensionamento do alimentador geral do quadro (docs/CALCULOS.md §3). */
public record ResultadoAlimentador(
        double potenciaTotalW,
        double potenciaTotalVA,
        double cargaDemandadaVA,
        double correnteTotalA,
        int tensaoV,
        int fases,
        ResultadoCircuito.Disjuntor disjuntorGeral,
        int correnteCalculadaA,
        Integer maiorDisjuntorCircuitoA,
        double secaoAlimentadorMm2,
        double secaoPorCapacidadeMm2,
        Double secaoMinimaPorRegraMm2,
        Double maiorSecaoCircuitoMm2,
        double secaoNeutroMm2,
        double secaoTerraMm2,
        String rotuloCabo,
        double quedaCalculadaPct,
        double capacidadeQuadroVA,
        int circuitosParalelos,
        double secaoFaseParaleloMm2) {
}
