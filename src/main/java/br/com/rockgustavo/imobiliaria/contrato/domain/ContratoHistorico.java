package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contrato_historico")
public class ContratoHistorico {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;

    @Column(nullable = false)
    private int versao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @Column(nullable = false)
    private UUID autor;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    protected ContratoHistorico() {
    }

    public ContratoHistorico(UUID contratoId, int versao, String snapshot, UUID autor) {
        this.id = IdGenerator.novoId();
        this.contratoId = Objects.requireNonNull(contratoId, "contratoId é obrigatório");
        this.versao = versao;
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot é obrigatório");
        this.autor = Objects.requireNonNull(autor, "autor é obrigatório");
        this.ocorridoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public int getVersao() {
        return versao;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public UUID getAutor() {
        return autor;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }
}
