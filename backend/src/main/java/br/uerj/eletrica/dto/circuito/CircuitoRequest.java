package br.uerj.eletrica.dto.circuito;

import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.TipoCircuito;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CircuitoRequest(
        @NotNull @Positive Integer numero,
        @Size(max = 120) String descricao,
        @NotNull TipoCircuito tipo,
        @NotNull Integer tensaoV,
        @NotNull @Min(1) @Max(3) Integer fases,
        // opcional quando há equipamentos: o total passa a ser derivado (docs/CALCULOS.md §1.1b)
        @Positive @DecimalMax("9999999999.99") BigDecimal potenciaW,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal fatorPotencia,
        @NotNull @Positive @DecimalMax("999999.99") BigDecimal comprimentoM,
        @NotNull @Min(1) Integer circuitosAgrupados,
        @NotNull Integer temperaturaC,
        @Min(1) Integer circuitosParalelos,
        MetodoInstalacao metodoInstalacao,
        Isolante isolante,
        @Min(1) Integer formaAgrupamentoRef,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal fatorDemanda,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("99.99") BigDecimal quedaAdmissivelPct,
        Boolean linhaSubterranea,
        @Size(max = 50) @Valid List<EquipamentoRequest> equipamentos) {

    /** Preenche os campos avançados omitidos com os padrões de docs/CALCULOS.md §1.1. */
    public CircuitoRequest comPadroes() {
        return new CircuitoRequest(
                numero, descricao, tipo, tensaoV, fases, potenciaW, fatorPotencia, comprimentoM,
                circuitosAgrupados, temperaturaC,
                circuitosParalelos != null ? circuitosParalelos : 1,
                metodoInstalacao != null ? metodoInstalacao : MetodoInstalacao.B1,
                isolante != null ? isolante : Isolante.PVC,
                formaAgrupamentoRef != null ? formaAgrupamentoRef : 1,
                fatorDemanda != null ? fatorDemanda : BigDecimal.ONE,
                quedaAdmissivelPct != null ? quedaAdmissivelPct : BigDecimal.valueOf(4),
                linhaSubterranea != null ? linhaSubterranea : Boolean.FALSE,
                equipamentos);
    }

    /** Request efetivo com a potência total derivada dos equipamentos (docs/CALCULOS.md §1.1b). */
    public CircuitoRequest comPotenciaW(BigDecimal potenciaDerivadaW) {
        return new CircuitoRequest(
                numero, descricao, tipo, tensaoV, fases, potenciaDerivadaW, fatorPotencia,
                comprimentoM, circuitosAgrupados, temperaturaC, circuitosParalelos,
                metodoInstalacao, isolante, formaAgrupamentoRef, fatorDemanda,
                quedaAdmissivelPct, linhaSubterranea, equipamentos);
    }
}
