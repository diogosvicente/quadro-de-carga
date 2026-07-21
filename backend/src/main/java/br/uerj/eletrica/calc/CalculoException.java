package br.uerj.eletrica.calc;

/**
 * Entrada válida em formato, mas impossível de dimensionar com as tabelas da NBR 5410
 * (ex.: temperatura sem fator tabelado, corrente acima do maior disjuntor).
 * Mapeada para HTTP 422 no ApiExceptionHandler.
 */
public class CalculoException extends RuntimeException {

    public CalculoException(String mensagem) {
        super(mensagem);
    }
}
