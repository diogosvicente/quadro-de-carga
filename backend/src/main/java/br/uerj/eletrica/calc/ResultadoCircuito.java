package br.uerj.eletrica.calc;

/** Resultado do dimensionamento de um circuito terminal (ver docs/CALCULOS.md §2.10). */
public record ResultadoCircuito(
        double potenciaVA,
        double correnteProjetoA,
        double correnteCorrigidaA,
        double fatorAgrupamento,
        double fatorTemperatura,
        double fatorTotal,
        Disjuntor disjuntor,
        double secaoSobrecorrenteMm2,
        double secaoQuedaCalculadaMm2,
        double secaoQuedaMm2,
        double secaoMinimaMm2,
        double secaoFinalMm2,
        double secaoNeutroMm2,
        double secaoTerraMm2,
        String rotuloCabo,
        double quedaCalculadaPct) {

    public record Disjuntor(int polos, int correnteA, String rotulo) {
    }
}
