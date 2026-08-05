package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "aditivo")
@EntityListeners(AuditingEntityListener.class)
public class Aditivo {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Column(name = "propriedade_id", nullable = false)
    private UUID propriedadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAditivo tipo;

    @Column(nullable = false)
    private String justificativa;

    @Column(nullable = false)
    private LocalDate data;

    @CreatedBy
    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Aditivo() {
    }

    Aditivo(Contrato contrato, UUID propriedadeId, TipoAditivo tipo, String justificativa) {
        this.id = IdGenerator.novoId();
        this.contrato = contrato;
        this.propriedadeId = Objects.requireNonNull(propriedadeId, "propriedadeId é obrigatório");
        this.tipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.justificativa = exigirNaoBranco(justificativa);
        this.data = LocalDate.now();
    }

    private static String exigirNaoBranco(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("justificativa é obrigatória");
        }
        return valor;
    }

    public UUID getPropriedadeId() {
        return propriedadeId;
    }

    public TipoAditivo getTipo() {
        return tipo;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public LocalDate getData() {
        return data;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
