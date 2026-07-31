package br.com.rockgustavo.imobiliaria.shared.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void definir(UUID tenantId) {
        TENANT_ATUAL.set(tenantId);
    }

    public static UUID obter() {
        return TENANT_ATUAL.get();
    }

    public static void limpar() {
        TENANT_ATUAL.remove();
    }
}
