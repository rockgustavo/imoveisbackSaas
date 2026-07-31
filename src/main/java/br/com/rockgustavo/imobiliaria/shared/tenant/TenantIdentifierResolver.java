package br.com.rockgustavo.imobiliaria.shared.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    static final UUID SEM_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID tenantId = TenantContext.obter();
        return tenantId != null ? tenantId : SEM_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
