package br.uerj.eletrica.service;

/** Violação de regra de negócio — mapeada para HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
