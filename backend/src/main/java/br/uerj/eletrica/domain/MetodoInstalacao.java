package br.uerj.eletrica.domain;

/**
 * Métodos de referência de instalação da NBR 5410 (Tabela 33).
 */
public enum MetodoInstalacao {
    A1("Condutores isolados em eletroduto embutido em parede termicamente isolante"),
    A2("Cabo multipolar em eletroduto embutido em parede termicamente isolante"),
    B1("Condutores isolados em eletroduto sobre parede ou embutido em alvenaria"),
    B2("Cabo multipolar em eletroduto sobre parede ou embutido em alvenaria"),
    C("Cabos uni ou multipolares sobre parede ou em bandeja não perfurada"),
    D("Cabos em eletroduto enterrado no solo"),
    E("Cabo multipolar ao ar livre ou em bandeja perfurada"),
    F("Cabos unipolares justapostos ao ar livre ou em bandeja perfurada"),
    G("Cabos unipolares espaçados ao ar livre");

    private final String descricao;

    MetodoInstalacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
