package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.geo.CepTestFixture;
import br.com.rockgustavo.imobiliaria.shared.geo.TestGeoConfig;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CepApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("RN-03-02/04, RN-04-01/03: consulta de CEP")
    class Consulta {

        @Test
        @DisplayName("CEP encontrado retorna endereço")
        void cepEncontradoRetornaEndereco() throws Exception {
            UUID tenantId = criarTenant();
            String cep = CepTestFixture.novoCep();

            mockMvc.perform(get("/api/v1/ceps/{cep}", cep).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encontrado").value(true))
                    .andExpect(jsonPath("$.localidade").value("Cidade Teste"));
        }

        @Test
        @DisplayName("RN-01: USUARIO também pode consultar")
        void usuarioTambemPodeConsultar() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(get("/api/v1/ceps/{cep}", CepTestFixture.novoCep()).with(usuarioDoTenant(tenantId)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("CEP não encontrado retorna 200 com encontrado=false — não é erro")
        void cepNaoEncontradoRetorna200() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(get("/api/v1/ceps/{cep}", TestGeoConfig.CEP_NAO_ENCONTRADO).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encontrado").value(false));
        }

        @Test
        @DisplayName("CEP mal formado retorna 200 com encontrado=false, sem chamar fornecedor")
        void cepMalFormadoRetorna200() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(get("/api/v1/ceps/{cep}", "123").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encontrado").value(false));
        }

        @Test
        @DisplayName("fornecedor indisponível retorna 502")
        void fornecedorIndisponivelRetorna502() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(get("/api/v1/ceps/{cep}", TestGeoConfig.CEP_PROVEDOR_INDISPONIVEL).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.codigo").value("CEP_PROVEDOR_INDISPONIVEL"));
        }

        @Test
        @DisplayName("RN-04-03: segunda consulta ao mesmo CEP usa o cache, não o fornecedor")
        void segundaConsultaUsaCache() throws Exception {
            UUID tenantId = criarTenant();
            String cep = CepTestFixture.novoCep();

            String primeiroLogradouro = mockMvc.perform(get("/api/v1/ceps/{cep}", cep).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            mockMvc.perform(get("/api/v1/ceps/{cep}", cep).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(content -> {
                        String segundo = content.getResponse().getContentAsString();
                        assertThat(segundo).isEqualTo(primeiroLogradouro);
                    });
        }
    }

    @Nested
    @DisplayName("Autorização e autenticação")
    class Autorizacao {

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/ceps/{cep}", CepTestFixture.novoCep()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/ceps/{cep}", CepTestFixture.novoCep()).with(jwt()))
                    .andExpect(status().isForbidden());
        }
    }

    private UUID criarTenant() throws Exception {
        String slug = "corretora-" + UUID.randomUUID();
        String corpo = """
                {"razaoSocial":"Corretora Teste","cnpj":"%s","slug":"%s"}
                """.formatted(CnpjTestFixture.novoCnpjValido(), slug);
        String location = mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                        .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private static RequestPostProcessor administradorDoTenant(UUID tenantId) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor usuarioDoTenant(UUID tenantId) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_USUARIO");
    }
}
