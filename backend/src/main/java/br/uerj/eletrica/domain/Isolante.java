package br.uerj.eletrica.domain;

public enum Isolante {
    PVC("PVC 70 °C"),
    EPR("EPR/XLPE 90 °C");

    private final String rotulo;

    Isolante(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
