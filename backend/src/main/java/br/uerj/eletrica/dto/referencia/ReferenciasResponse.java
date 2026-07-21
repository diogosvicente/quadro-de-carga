package br.uerj.eletrica.dto.referencia;

import java.util.List;

/** Listas para popular os selects do frontend (GET /api/referencias). */
public record ReferenciasResponse(
        List<Integer> tensoes,
        List<OpcaoFase> fases,
        List<OpcaoTipoCircuito> tiposCircuito,
        List<OpcaoIsolante> isolantes,
        List<OpcaoMetodoInstalacao> metodosInstalacao,
        List<OpcaoSistemaTensao> sistemasTensao,
        List<OpcaoFormaAgrupamento> formasAgrupamento) {

    public record OpcaoFase(int valor, String rotulo) {}

    public record OpcaoTipoCircuito(String codigo, String rotulo, double secaoMinimaMm2) {}

    public record OpcaoIsolante(String codigo, String rotulo) {}

    public record OpcaoMetodoInstalacao(String codigo, String descricao) {}

    public record OpcaoSistemaTensao(String codigo, int faseNeutroV, int faseFaseV, String rotulo) {}

    public record OpcaoFormaAgrupamento(int ref, String descricao) {}
}
