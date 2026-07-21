package br.uerj.eletrica.web;

import br.uerj.eletrica.calc.CalculoException;
import br.uerj.eletrica.service.RecursoNaoEncontradoException;
import br.uerj.eletrica.service.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envelope único de erro da API: { "status", "mensagem", "erros": { campo: detalhe } | null }.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErroResponse(int status, String mensagem, Map<String, String> erros) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse validacao(MethodArgumentNotValidException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> erros.putIfAbsent(f.getField(), mensagemCampo(f.getCode(), f.getDefaultMessage())));
        return new ErroResponse(400, "Verifique os campos destacados.", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse corpoIlegivel(HttpMessageNotReadableException e) {
        return new ErroResponse(400, "Corpo da requisição inválido (verifique tipos: inteiros sem casas decimais).", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse parametroInvalido(MethodArgumentTypeMismatchException e) {
        return new ErroResponse(400, "Parâmetro inválido na URL: " + e.getName() + ".", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse rotaInexistente(NoResourceFoundException e) {
        return new ErroResponse(404, "Rota não encontrada.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse integridade(DataIntegrityViolationException e) {
        // corrida em constraint de unicidade ou valor fora da precisão da coluna
        return new ErroResponse(422, "Registro conflita com outro já existente ou tem valor fora do limite aceito.", null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse naoEncontrado(RecursoNaoEncontradoException e) {
        return new ErroResponse(404, e.getMessage(), null);
    }

    @ExceptionHandler({RegraDeNegocioException.class, CalculoException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse regraDeNegocio(RuntimeException e) {
        return new ErroResponse(422, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResponse inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return new ErroResponse(500, "Erro interno. Tente novamente.", null);
    }

    private static String mensagemCampo(String codigo, String padrao) {
        return switch (codigo == null ? "" : codigo) {
            case "NotNull", "NotBlank" -> "Campo obrigatório.";
            case "Positive" -> "Deve ser maior que zero.";
            case "PositiveOrZero" -> "Não pode ser negativo.";
            case "Min", "DecimalMin" -> "Valor abaixo do mínimo aceito.";
            case "Max", "DecimalMax" -> "Valor acima do máximo aceito.";
            case "Size" -> "Tamanho fora do limite.";
            default -> padrao != null ? padrao : "Valor inválido.";
        };
    }
}
