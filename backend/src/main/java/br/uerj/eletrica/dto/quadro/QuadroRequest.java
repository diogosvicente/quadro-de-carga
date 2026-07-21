package br.uerj.eletrica.dto.quadro;

import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuadroRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 200) String local,
        @NotNull SistemaTensao sistemaTensao,
        @NotNull @Min(1) @Max(3) Integer fasesAlimentador,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") BigDecimal fatorDemanda,
        @NotNull @PositiveOrZero @DecimalMax("9999999999.99") BigDecimal cargaReservaVA,
        @NotNull @Positive @DecimalMax("999999.99") BigDecimal comprimentoM,
        @NotNull MetodoInstalacao metodoInstalacao,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("99.99") BigDecimal quedaAdmissivelPct,
        @NotNull Integer temperaturaC) {
}
