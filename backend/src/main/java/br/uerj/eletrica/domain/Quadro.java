package br.uerj.eletrica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "quadro")
public class Quadro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "local", length = 200)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(name = "sistema_tensao", nullable = false, length = 20)
    private SistemaTensao sistemaTensao = SistemaTensao.V127_220;

    @Column(name = "fases_alimentador", nullable = false)
    private Integer fasesAlimentador = 3;

    @Column(name = "circuitos_paralelos", nullable = false)
    private Integer circuitosParalelos = 1;

    @Column(name = "fator_demanda", nullable = false)
    private BigDecimal fatorDemanda = BigDecimal.ONE;

    @Column(name = "carga_reserva_va", nullable = false)
    private BigDecimal cargaReservaVA = BigDecimal.ZERO;

    @Column(name = "comprimento_m", nullable = false)
    private BigDecimal comprimentoM = BigDecimal.TEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_instalacao", nullable = false, length = 4)
    private MetodoInstalacao metodoInstalacao = MetodoInstalacao.B1;

    @Column(name = "queda_admissivel_pct", nullable = false)
    private BigDecimal quedaAdmissivelPct = BigDecimal.valueOf(2);

    @Column(name = "temperatura_c", nullable = false)
    private Integer temperaturaC = 30;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public SistemaTensao getSistemaTensao() { return sistemaTensao; }
    public void setSistemaTensao(SistemaTensao sistemaTensao) { this.sistemaTensao = sistemaTensao; }
    public Integer getFasesAlimentador() { return fasesAlimentador; }
    public void setFasesAlimentador(Integer fasesAlimentador) { this.fasesAlimentador = fasesAlimentador; }
    public Integer getCircuitosParalelos() { return circuitosParalelos; }
    public void setCircuitosParalelos(Integer circuitosParalelos) { this.circuitosParalelos = circuitosParalelos; }
    public BigDecimal getFatorDemanda() { return fatorDemanda; }
    public void setFatorDemanda(BigDecimal fatorDemanda) { this.fatorDemanda = fatorDemanda; }
    public BigDecimal getCargaReservaVA() { return cargaReservaVA; }
    public void setCargaReservaVA(BigDecimal cargaReservaVA) { this.cargaReservaVA = cargaReservaVA; }
    public BigDecimal getComprimentoM() { return comprimentoM; }
    public void setComprimentoM(BigDecimal comprimentoM) { this.comprimentoM = comprimentoM; }
    public MetodoInstalacao getMetodoInstalacao() { return metodoInstalacao; }
    public void setMetodoInstalacao(MetodoInstalacao metodoInstalacao) { this.metodoInstalacao = metodoInstalacao; }
    public BigDecimal getQuedaAdmissivelPct() { return quedaAdmissivelPct; }
    public void setQuedaAdmissivelPct(BigDecimal quedaAdmissivelPct) { this.quedaAdmissivelPct = quedaAdmissivelPct; }
    public Integer getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(Integer temperaturaC) { this.temperaturaC = temperaturaC; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
