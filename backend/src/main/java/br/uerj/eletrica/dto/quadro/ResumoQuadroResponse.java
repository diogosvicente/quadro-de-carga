package br.uerj.eletrica.dto.quadro;

import br.uerj.eletrica.calc.ResultadoAlimentador;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;

import java.util.List;

/** Resumo do quadro: alimentador geral (null quando não há circuitos) + circuitos calculados. */
public record ResumoQuadroResponse(
        QuadroResponse quadro,
        ResultadoAlimentador alimentador,
        List<CircuitoResponse> circuitos) {
}
