package br.uerj.eletrica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "circuito")
public class Circuito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quadro_id")
    private Quadro quadro;

    @Column(nullable = false)
    private Integer numero;

    @Column(length = 120)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCircuito tipo = TipoCircuito.FORCA;

    @Column(name = "tensao_v", nullable = false)
    private Integer tensaoV;

    @Column(nullable = false)
    private Integer fases = 1;

    @Column(name = "potencia_w", nullable = false)
    private BigDecimal potenciaW;

    @Column(name = "fator_potencia", nullable = false)
    private BigDecimal fatorPotencia = BigDecimal.valueOf(0.92);

    @Column(name = "comprimento_m", nullable = false)
    private BigDecimal comprimentoM;

    @Column(name = "circuitos_agrupados", nullable = false)
    private Integer circuitosAgrupados = 1;

    @Column(name = "circuitos_paralelos", nullable = false)
    private Integer circuitosParalelos = 1;

    @Column(name = "temperatura_c", nullable = false)
    private Integer temperaturaC = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_instalacao", nullable = false, length = 4)
    private MetodoInstalacao metodoInstalacao = MetodoInstalacao.B1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Isolante isolante = Isolante.PVC;

    @Column(name = "forma_agrupamento_ref", nullable = false)
    private Integer formaAgrupamentoRef = 1;

    @Column(name = "fator_demanda", nullable = false)
    private BigDecimal fatorDemanda = BigDecimal.ONE;

    @Column(name = "queda_admissivel_pct", nullable = false)
    private BigDecimal quedaAdmissivelPct = BigDecimal.valueOf(4);

    @Column(name = "linha_subterranea", nullable = false)
    private Boolean linhaSubterranea = Boolean.FALSE;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Quadro getQuadro() { return quadro; }
    public void setQuadro(Quadro quadro) { this.quadro = quadro; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public TipoCircuito getTipo() { return tipo; }
    public void setTipo(TipoCircuito tipo) { this.tipo = tipo; }
    public Integer getTensaoV() { return tensaoV; }
    public void setTensaoV(Integer tensaoV) { this.tensaoV = tensaoV; }
    public Integer getFases() { return fases; }
    public void setFases(Integer fases) { this.fases = fases; }
    public BigDecimal getPotenciaW() { return potenciaW; }
    public void setPotenciaW(BigDecimal potenciaW) { this.potenciaW = potenciaW; }
    public BigDecimal getFatorPotencia() { return fatorPotencia; }
    public void setFatorPotencia(BigDecimal fatorPotencia) { this.fatorPotencia = fatorPotencia; }
    public BigDecimal getComprimentoM() { return comprimentoM; }
    public void setComprimentoM(BigDecimal comprimentoM) { this.comprimentoM = comprimentoM; }
    public Integer getCircuitosAgrupados() { return circuitosAgrupados; }
    public void setCircuitosAgrupados(Integer circuitosAgrupados) { this.circuitosAgrupados = circuitosAgrupados; }
    public Integer getCircuitosParalelos() { return circuitosParalelos; }
    public void setCircuitosParalelos(Integer circuitosParalelos) { this.circuitosParalelos = circuitosParalelos; }
    public Integer getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(Integer temperaturaC) { this.temperaturaC = temperaturaC; }
    public MetodoInstalacao getMetodoInstalacao() { return metodoInstalacao; }
    public void setMetodoInstalacao(MetodoInstalacao metodoInstalacao) { this.metodoInstalacao = metodoInstalacao; }
    public Isolante getIsolante() { return isolante; }
    public void setIsolante(Isolante isolante) { this.isolante = isolante; }
    public Integer getFormaAgrupamentoRef() { return formaAgrupamentoRef; }
    public void setFormaAgrupamentoRef(Integer formaAgrupamentoRef) { this.formaAgrupamentoRef = formaAgrupamentoRef; }
    public BigDecimal getFatorDemanda() { return fatorDemanda; }
    public void setFatorDemanda(BigDecimal fatorDemanda) { this.fatorDemanda = fatorDemanda; }
    public BigDecimal getQuedaAdmissivelPct() { return quedaAdmissivelPct; }
    public void setQuedaAdmissivelPct(BigDecimal quedaAdmissivelPct) { this.quedaAdmissivelPct = quedaAdmissivelPct; }
    public Boolean getLinhaSubterranea() { return linhaSubterranea; }
    public void setLinhaSubterranea(Boolean linhaSubterranea) { this.linhaSubterranea = linhaSubterranea; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
