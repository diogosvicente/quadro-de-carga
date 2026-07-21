package br.uerj.eletrica.domain;

/**
 * Utilização do circuito — define a seção mínima normativa (NBR 5410, Tabela 47).
 * Tomadas de corrente contam como circuitos de força (nota 2 da tabela).
 */
public enum TipoCircuito {
    ILUMINACAO("Iluminação", 1.5),
    FORCA("Força", 2.5),
    SINALIZACAO("Sinalização ou controle", 0.5);

    private final String rotulo;
    private final double secaoMinimaMm2;

    TipoCircuito(String rotulo, double secaoMinimaMm2) {
        this.rotulo = rotulo;
        this.secaoMinimaMm2 = secaoMinimaMm2;
    }

    public String getRotulo() {
        return rotulo;
    }

    public double getSecaoMinimaMm2() {
        return secaoMinimaMm2;
    }
}
