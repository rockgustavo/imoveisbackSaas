package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(name = "imobiliaria", uniqueConstraints = {
        @UniqueConstraint(name = "uq_imobiliaria_cnpj", columnNames = "cnpj"),
        @UniqueConstraint(name = "uq_imobiliaria_slug", columnNames = "slug")
})
public class Imobiliaria extends Auditable {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    @Id
    private UUID id;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(nullable = false, length = 60)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusImobiliaria status;

    protected Imobiliaria() {
    }

    public Imobiliaria(String razaoSocial, String cnpj, String slug) {
        this.id = IdGenerator.novoId();
        this.razaoSocial = exigirNaoVazio(razaoSocial, "razaoSocial");
        this.cnpj = validarCnpj(cnpj);
        this.slug = validarSlug(slug);
        this.status = StatusImobiliaria.ATIVA;
    }

    private static String exigirNaoVazio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    private static String validarCnpj(String cnpj) {
        String digitos = cnpj == null ? null : cnpj.replaceAll("\\D", "");
        if (!CnpjValidator.valido(digitos)) {
            throw new CnpjInvalidoException(cnpj);
        }
        return digitos;
    }

    private static String validarSlug(String slug) {
        if (slug == null || !SLUG_VALIDO.matcher(slug).matches()) {
            throw new SlugInvalidoException(slug);
        }
        return slug;
    }

    public UUID getId() {
        return id;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getSlug() {
        return slug;
    }

    public StatusImobiliaria getStatus() {
        return status;
    }
}
