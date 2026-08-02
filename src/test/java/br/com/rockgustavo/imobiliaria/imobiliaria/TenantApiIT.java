package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.CriarImobiliariaComando;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaService;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ImobiliariaService imobiliariaService;

    @Nested
    @DisplayName("RN-00-06: identidade do tenant corrente")
    class IdentidadeDoTenant {

        @Test
        @DisplayName("devolve a razão social da imobiliária do token")
        void devolveRazaoSocialDoTenantDoToken() throws Exception {
            UUID tenantId = criarTenant("Corretora Alfa Ltda");

            mockMvc.perform(get("/api/v1/tenant").with(usuarioDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(tenantId.toString()))
                    .andExpect(jsonPath("$.razaoSocial").value("Corretora Alfa Ltda"))
                    .andExpect(jsonPath("$.status").value("ATIVA"));
        }

        @Test
        @DisplayName("cada tenant enxerga apenas a própria razão social")
        void cadaTenantEnxergaApenasAPropria() throws Exception {
            UUID tenantA = criarTenant("Corretora Alfa Ltda");
            UUID tenantB = criarTenant("Corretora Beta Ltda");

            mockMvc.perform(get("/api/v1/tenant").with(usuarioDoTenant(tenantA)))
                    .andExpect(jsonPath("$.razaoSocial").value("Corretora Alfa Ltda"));

            mockMvc.perform(get("/api/v1/tenant").with(usuarioDoTenant(tenantB)))
                    .andExpect(jsonPath("$.razaoSocial").value("Corretora Beta Ltda"));
        }

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/tenant"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO ou ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/tenant").with(jwt()))
                    .andExpect(status().isForbidden());
        }
    }

    private UUID criarTenant(String razaoSocial) {
        String slug = "corretora-" + UUID.randomUUID();
        return imobiliariaService.criar(
                new CriarImobiliariaComando(razaoSocial, CnpjTestFixture.novoCnpjValido(), slug));
    }

    private static RequestPostProcessor usuarioDoTenant(UUID tenantId) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_USUARIO");
    }
}
