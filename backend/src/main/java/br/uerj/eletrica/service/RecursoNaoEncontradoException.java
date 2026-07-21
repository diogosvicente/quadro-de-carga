package br.uerj.eletrica.service;

/** Recurso inexistente — mapeada para HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
