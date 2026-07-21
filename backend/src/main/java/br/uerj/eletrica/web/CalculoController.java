package br.uerj.eletrica.web;

import br.uerj.eletrica.calc.ResultadoCircuito;
import br.uerj.eletrica.dto.circuito.CircuitoRequest;
import br.uerj.eletrica.service.CircuitoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculos")
public class CalculoController {

    private final CircuitoService service;

    public CalculoController(CircuitoService service) {
        this.service = service;
    }

    /** Prévia do dimensionamento sem persistir nada. */
    @PostMapping("/circuito")
    public ResultadoCircuito circuito(@Valid @RequestBody CircuitoRequest req) {
        return service.calcularPrevia(req);
    }
}
