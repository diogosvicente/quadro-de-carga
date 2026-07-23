package br.uerj.eletrica.calc;

import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;

/**
 * Entradas do dimensionamento do alimentador do quadro (ver docs/CALCULOS.md §3).
 * {@code maiorDisjuntorCircuitoA}/{@code maiorSecaoCircuitoMm2} vêm dos circuitos já calculados
 * e alimentam a regra do "um valor padrão acima" como mínimo.
 */
public record EntradaAlimentador(
        SistemaTensao sistemaTensao,
        int fases,
        double fatorDemanda,
        double cargaReservaVA,
        double comprimentoM,
        MetodoInstalacao metodoInstalacao,
        double quedaAdmissivelPct,
        int temperaturaC,
        double somaPotenciaW,
        double somaPotenciaVA,
        Integer maiorDisjuntorCircuitoA,
        Double maiorSecaoCircuitoMm2,
        int circuitosParalelos) {
}
