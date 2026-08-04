package br.uerj.eletrica.dto.circuito;

import br.uerj.eletrica.domain.Equipamento;

import java.math.BigDecimal;

/** Equipamento do circuito devolvido pela API, na ordem cadastrada (docs/CALCULOS.md §1.1b). */
public record EquipamentoResponse(
        Long id,
        String nome,
        Integer quantidade,
        BigDecimal potenciaW,
        BigDecimal correnteA) {

    public static EquipamentoResponse de(Equipamento e) {
        return new EquipamentoResponse(e.getId(), e.getNome(), e.getQuantidade(),
                e.getPotenciaW(), e.getCorrenteA());
    }
}
