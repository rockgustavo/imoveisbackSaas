package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.CriarImobiliariaComando;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaService;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParametrosTenantApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ImobiliariaService imobiliariaService;

    @Nested
    @DisplayName("RN-00-09/10: parâmetros do tenant")
    class ParametrosDoTenant {

        @Test
        @DisplayName("nasce com os defaults de plataforma")
        void nasceComDefaults() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(get("/api/v1/tenant/parametros").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comissaoPercentualTeto").value("6.00"))
                    .andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"));
        }

        @Test
        @DisplayName("RN-00-01/02/03: alteração em um tenant não vaza nem afeta outro")
        void isolaEntreTenants() throws Exception {
            UUID tenantA = criarTenant();
            UUID tenantB = criarTenant();

            mockMvc.perform(put("/api/v1/tenant/parametros")
                            .with(administradorDoTenant(tenantA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"comissaoPercentualTeto":9.50,"orcamentoValidadeDiasPadrao":10,
                                     "geocodificacaoTentativasMax":3,"cepCacheJanelaDias":60,"fusoHorario":"America/Fortaleza"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comissaoPercentualTeto").value("9.50"));

            mockMvc.perform(get("/api/v1/tenant/parametros").with(administradorDoTenant(tenantB)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comissaoPercentualTeto").value("6.00"))
                    .andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"));
        }

        @Test
        @DisplayName("rejeita teto de comissão zero com 422")
        void rejeitaTetoInvalido() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(put("/api/v1/tenant/parametros")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"comissaoPercentualTeto":0,"orcamentoValidadeDiasPadrao":10,
                                     "geocodificacaoTentativasMax":3,"cepCacheJanelaDias":60,"fusoHorario":"America/Fortaleza"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    private UUID criarTenant() {
        String slug = "corretora-" + UUID.randomUUID();
        return imobiliariaService.criar(new CriarImobiliariaComando("Corretora Teste", CnpjTestFixture.novoCnpjValido(), slug));
    }

    private static RequestPostProcessor administradorDoTenant(UUID tenantId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_ADMINISTRADOR");
    }
}
