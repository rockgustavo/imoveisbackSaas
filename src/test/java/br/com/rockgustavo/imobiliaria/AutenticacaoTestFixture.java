package br.com.rockgustavo.imobiliaria;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class AutenticacaoTestFixture {

    private AutenticacaoTestFixture() {
    }

    public static RequestPostProcessor plataformaAdmin() {
        return jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN");
    }

    public static RequestPostProcessor administradorDoTenant(UUID tenantId) {
        return doTenant(tenantId, "ROLE_ADMINISTRADOR");
    }

    public static RequestPostProcessor usuarioDoTenant(UUID tenantId) {
        return doTenant(tenantId, "ROLE_USUARIO");
    }

    public static RequestPostProcessor comoPessoa(UUID tenantId, String subjectIdp) {
        return jwt()
                .jwt(builder -> builder.subject(subjectIdp).claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor doTenant(UUID tenantId, String papel) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> papel);
    }
}
