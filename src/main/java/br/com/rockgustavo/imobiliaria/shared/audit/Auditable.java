package br.com.rockgustavo.imobiliaria.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @CreatedBy
    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @LastModifiedDate
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    @LastModifiedBy
    @Column(name = "alterado_por", nullable = false)
    private UUID alteradoPor;

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public UUID getCriadoPor() {
        return criadoPor;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public UUID getAlteradoPor() {
        return alteradoPor;
    }
}
