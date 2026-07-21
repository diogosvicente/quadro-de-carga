package br.uerj.eletrica.web;

import br.uerj.eletrica.dto.quadro.QuadroRequest;
import br.uerj.eletrica.dto.quadro.QuadroResponse;
import br.uerj.eletrica.dto.quadro.ResumoQuadroResponse;
import br.uerj.eletrica.service.QuadroService;
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
@RequestMapping("/api/quadros")
public class QuadroController {

    private final QuadroService service;

    public QuadroController(QuadroService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuadroResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuadroResponse criar(@Valid @RequestBody QuadroRequest req) {
        return service.criar(req);
    }

    @GetMapping("/{id}")
    public QuadroResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public QuadroResponse atualizar(@PathVariable Long id, @Valid @RequestBody QuadroRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @GetMapping("/{id}/resumo")
    public ResumoQuadroResponse resumo(@PathVariable Long id) {
        return service.resumo(id);
    }
}
