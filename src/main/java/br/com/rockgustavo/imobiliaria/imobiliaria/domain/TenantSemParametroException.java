package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TenantSemParametroException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TenantSemParametroException(UUID tenantId) {
        super("TENANT_SEM_PARAMETRO", HttpStatus.INTERNAL_SERVER_ERROR,
                "Tenant sem parâmetros configurados: " + tenantId);
    }
}
