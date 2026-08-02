package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TenantNaoEncontradoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TenantNaoEncontradoException(UUID tenantId) {
        super("TENANT_NAO_ENCONTRADO", HttpStatus.NOT_FOUND, "Tenant não encontrado: " + tenantId);
    }
}
