package br.uerj.eletrica.calc;

import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.TipoCircuito;

/** Entradas do dimensionamento de um circuito terminal (ver docs/CALCULOS.md §1.1). */
public record EntradaCircuito(
        TipoCircuito tipo,
        int tensaoV,
        int fases,
        double potenciaW,
        double fatorPotencia,
        double comprimentoM,
        int circuitosAgrupados,
        int temperaturaC,
        MetodoInstalacao metodoInstalacao,
        Isolante isolante,
        int formaAgrupamentoRef,
        double fatorDemanda,
        double quedaAdmissivelPct,
        boolean linhaSubterranea) {
}
