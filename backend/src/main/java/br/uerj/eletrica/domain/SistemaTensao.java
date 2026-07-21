package br.uerj.eletrica.domain;

/**
 * Sistemas de tensão suportados pela planilha de origem (célula BI111): F-N / F-F.
 */
public enum SistemaTensao {
    V127_220(127, 220, "127/220 V"),
    V220_380(220, 380, "220/380 V"),
    V254_440(254, 440, "254/440 V");

    private final int faseNeutroV;
    private final int faseFaseV;
    private final String rotulo;

    SistemaTensao(int faseNeutroV, int faseFaseV, String rotulo) {
        this.faseNeutroV = faseNeutroV;
        this.faseFaseV = faseFaseV;
        this.rotulo = rotulo;
    }

    public int getFaseNeutroV() {
        return faseNeutroV;
    }

    public int getFaseFaseV() {
        return faseFaseV;
    }

    public String getRotulo() {
        return rotulo;
    }
}
