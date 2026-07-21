package br.uerj.eletrica.web;

import br.uerj.eletrica.dto.referencia.ReferenciasResponse;
import br.uerj.eletrica.service.ReferenciaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referencias")
public class ReferenciaController {

    private final ReferenciaService service;

    public ReferenciaController(ReferenciaService service) {
        this.service = service;
    }

    @GetMapping
    public ReferenciasResponse listar() {
        return service.montar();
    }
}
