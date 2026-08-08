package br.com.rockgustavo.imobiliaria.shared.auditoria;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "historico_transicao")
public class HistoricoTransicao {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidade_tipo", nullable = false, length = 20)
    private EntidadeAuditavel entidadeTipo;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(name = "status_anterior", length = 20)
    private String statusAnterior;

    @Column(name = "status_novo", nullable = false, length = 20)
    private String statusNovo;

    @Column(nullable = false)
    private UUID autor;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    protected HistoricoTransicao() {
    }

    public HistoricoTransicao(EntidadeAuditavel entidadeTipo, UUID entidadeId, String statusAnterior,
                               String statusNovo, UUID autor) {
        this.id = IdGenerator.novoId();
        this.entidadeTipo = Objects.requireNonNull(entidadeTipo, "entidadeTipo é obrigatório");
        this.entidadeId = Objects.requireNonNull(entidadeId, "entidadeId é obrigatório");
        this.statusAnterior = statusAnterior;
        this.statusNovo = Objects.requireNonNull(statusNovo, "statusNovo é obrigatório");
        this.autor = Objects.requireNonNull(autor, "autor é obrigatório");
        this.ocorridoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public EntidadeAuditavel getEntidadeTipo() {
        return entidadeTipo;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public String getStatusNovo() {
        return statusNovo;
    }

    public UUID getAutor() {
        return autor;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }
}
