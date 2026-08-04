package br.uerj.eletrica.dto.circuito;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Item da lista de equipamentos do circuito (docs/CALCULOS.md §1.1b).
 * Exatamente um entre {@code potenciaW} e {@code correnteA} deve ser informado —
 * regra de negócio validada no CircuitoService (422).
 */
public record EquipamentoRequest(
        @Size(max = 120) String nome,
        @NotNull @Min(1) Integer quantidade,
        @Positive @DecimalMax("9999999999.99") BigDecimal potenciaW,
        @Positive @DecimalMax("99999.99") BigDecimal correnteA) {
}
