package br.uerj.eletrica.service;

import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import br.uerj.eletrica.domain.Isolante;
import br.uerj.eletrica.domain.MetodoInstalacao;
import br.uerj.eletrica.domain.SistemaTensao;
import br.uerj.eletrica.domain.TipoCircuito;
import br.uerj.eletrica.dto.referencia.ReferenciasResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ReferenciaService {

    private final TabelasNbr5410 tabelas;

    public ReferenciaService(TabelasNbr5410 tabelas) {
        this.tabelas = tabelas;
    }

    public ReferenciasResponse montar() {
        return new ReferenciasResponse(
                List.of(127, 220, 380, 440, 660),
                List.of(new ReferenciasResponse.OpcaoFase(1, "Monofásico (1P)"),
                        new ReferenciasResponse.OpcaoFase(2, "Bifásico (2P)"),
                        new ReferenciasResponse.OpcaoFase(3, "Trifásico (3P)")),
                Arrays.stream(TipoCircuito.values())
                        .map(t -> new ReferenciasResponse.OpcaoTipoCircuito(t.name(), t.getRotulo(), t.getSecaoMinimaMm2()))
                        .toList(),
                Arrays.stream(Isolante.values())
                        .map(i -> new ReferenciasResponse.OpcaoIsolante(i.name(), i.getRotulo()))
                        .toList(),
                Arrays.stream(MetodoInstalacao.values())
                        .map(m -> new ReferenciasResponse.OpcaoMetodoInstalacao(m.name(), m.getDescricao()))
                        .toList(),
                Arrays.stream(SistemaTensao.values())
                        .map(s -> new ReferenciasResponse.OpcaoSistemaTensao(
                                s.name(), s.getFaseNeutroV(), s.getFaseFaseV(), s.getRotulo()))
                        .toList(),
                tabelas.formasAgrupamento().stream()
                        .map(f -> new ReferenciasResponse.OpcaoFormaAgrupamento(f.ref(), f.descricao()))
                        .toList());
    }
}
