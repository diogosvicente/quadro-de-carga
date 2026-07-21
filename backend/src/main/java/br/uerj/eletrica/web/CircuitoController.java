package br.uerj.eletrica.web;

import br.uerj.eletrica.dto.circuito.CircuitoRequest;
import br.uerj.eletrica.dto.circuito.CircuitoResponse;
import br.uerj.eletrica.service.CircuitoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quadros/{quadroId}/circuitos")
public class CircuitoController {

    private final CircuitoService service;

    public CircuitoController(CircuitoService service) {
        this.service = service;
    }

    @GetMapping
    public List<CircuitoResponse> listar(@PathVariable Long quadroId) {
        return service.listar(quadroId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CircuitoResponse criar(@PathVariable Long quadroId, @Valid @RequestBody CircuitoRequest req) {
        return service.criar(quadroId, req);
    }

    @GetMapping("/{circuitoId}")
    public CircuitoResponse buscar(@PathVariable Long quadroId, @PathVariable Long circuitoId) {
        return service.buscar(quadroId, circuitoId);
    }

    @PutMapping("/{circuitoId}")
    public CircuitoResponse atualizar(@PathVariable Long quadroId, @PathVariable Long circuitoId,
                                      @Valid @RequestBody CircuitoRequest req) {
        return service.atualizar(quadroId, circuitoId, req);
    }

    @DeleteMapping("/{circuitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long quadroId, @PathVariable Long circuitoId) {
        service.remover(quadroId, circuitoId);
    }
}
