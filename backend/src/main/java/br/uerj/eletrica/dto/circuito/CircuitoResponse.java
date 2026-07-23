package br.uerj.eletrica.dto.circuito;

import br.uerj.eletrica.calc.ResultadoCircuito;
import br.uerj.eletrica.domain.Circuito;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.TipoCircuito;

import java.math.BigDecimal;

public record CircuitoResponse(
        Long id,
        Long quadroId,
        Integer numero,
        String descricao,
        TipoCircuito tipo,
        Integer tensaoV,
        Integer fases,
        BigDecimal potenciaW,
        BigDecimal fatorPotencia,
        BigDecimal comprimentoM,
        Integer circuitosAgrupados,
        Integer circuitosParalelos,
        Integer temperaturaC,
        MetodoInstalacao metodoInstalacao,
        Isolante isolante,
        Integer formaAgrupamentoRef,
        BigDecimal fatorDemanda,
        BigDecimal quedaAdmissivelPct,
        Boolean linhaSubterranea,
        ResultadoCircuito resultado) {

    public static CircuitoResponse de(Circuito c, ResultadoCircuito resultado) {
        return new CircuitoResponse(
                c.getId(), c.getQuadro().getId(), c.getNumero(), c.getDescricao(), c.getTipo(),
                c.getTensaoV(), c.getFases(), c.getPotenciaW(), c.getFatorPotencia(), c.getComprimentoM(),
                c.getCircuitosAgrupados(), c.getCircuitosParalelos(), c.getTemperaturaC(),
                c.getMetodoInstalacao(), c.getIsolante(),
                c.getFormaAgrupamentoRef(), c.getFatorDemanda(), c.getQuedaAdmissivelPct(),
                c.getLinhaSubterranea(), resultado);
    }
}
