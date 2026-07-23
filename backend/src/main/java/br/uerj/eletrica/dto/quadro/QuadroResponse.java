package br.uerj.eletrica.dto.quadro;

import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.Quadro;
import br.uerj.eletrica.domain.SistemaTensao;

import java.math.BigDecimal;

public record QuadroResponse(
        Long id,
        String nome,
        String local,
        SistemaTensao sistemaTensao,
        Integer fasesAlimentador,
        Integer circuitosParalelos,
        BigDecimal fatorDemanda,
        BigDecimal cargaReservaVA,
        BigDecimal comprimentoM,
        MetodoInstalacao metodoInstalacao,
        BigDecimal quedaAdmissivelPct,
        Integer temperaturaC,
        long totalCircuitos,
        BigDecimal potenciaTotalW) {

    public static QuadroResponse de(Quadro q, long totalCircuitos, BigDecimal potenciaTotalW) {
        return new QuadroResponse(
                q.getId(), q.getNome(), q.getLocal(), q.getSistemaTensao(), q.getFasesAlimentador(),
                q.getCircuitosParalelos(), q.getFatorDemanda(), q.getCargaReservaVA(), q.getComprimentoM(),
                q.getMetodoInstalacao(),
                q.getQuedaAdmissivelPct(), q.getTemperaturaC(), totalCircuitos, potenciaTotalW);
    }
}
