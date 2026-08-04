package br.uerj.eletrica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Item da lista opcional de equipamentos de um circuito (docs/CALCULOS.md §1.1b):
 * quantidade × valor unitário (potência OU corrente — exatamente um dos dois).
 */
@Entity
@Table(name = "equipamento")
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuito_id")
    private Circuito circuito;

    @Column(length = 120)
    private String nome;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "potencia_w")
    private BigDecimal potenciaW;

    @Column(name = "corrente_a")
    private BigDecimal correnteA;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Circuito getCircuito() { return circuito; }
    public void setCircuito(Circuito circuito) { this.circuito = circuito; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPotenciaW() { return potenciaW; }
    public void setPotenciaW(BigDecimal potenciaW) { this.potenciaW = potenciaW; }
    public BigDecimal getCorrenteA() { return correnteA; }
    public void setCorrenteA(BigDecimal correnteA) { this.correnteA = correnteA; }
    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
