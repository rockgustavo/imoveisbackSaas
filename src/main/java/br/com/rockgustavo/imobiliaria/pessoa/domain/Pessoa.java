package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TenantId;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pessoa", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pessoa_documento", columnNames = {"tenant_id", "tipo_documento", "documento"})
})
public class Pessoa extends Auditable {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 14)
    private String documento;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(length = 200)
    private String email;

    @Column(name = "subject_idp", length = 100)
    private String subjectIdp;

    @Column(nullable = false)
    private boolean ativo;

    protected Pessoa() {
    }

    public Pessoa(TipoDocumento tipoDocumento, String documento, String nome, String email) {
        this.id = IdGenerator.novoId();
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "tipoDocumento é obrigatório");
        this.documento = DocumentoValidator.validar(tipoDocumento, documento);
        this.nome = exigirNaoVazio(nome, "nome");
        this.email = email;
        this.ativo = true;
    }

    private static String exigirNaoVazio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    public void atualizar(String nome, String email) {
        this.nome = exigirNaoVazio(nome, "nome");
        this.email = email;
    }

    public void provisionarCredencial(String subjectIdp) {
        this.subjectIdp = subjectIdp;
    }

    public void inativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSubjectIdp() {
        return subjectIdp;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
