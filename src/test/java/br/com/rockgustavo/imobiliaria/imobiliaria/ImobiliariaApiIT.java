package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImobiliariaApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("RN-00-06/07/08: provisionamento de imobiliária")
    class ProvisionamentoDeTenant {

        @Test
        @DisplayName("cria imobiliária com CNPJ e slug válidos")
        void criaImobiliaria() throws Exception {
            String corpo = """
                    {"razaoSocial":"Corretora Exemplo Ltda","cnpj":"%s","slug":"corretora-exemplo-%s"}
                    """.formatted(CnpjTestFixture.novoCnpjValido(), System.nanoTime());

            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("rejeita CNPJ com dígito verificador inválido")
        void rejeitaCnpjInvalido() throws Exception {
            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"razaoSocial":"Corretora Exemplo Ltda","cnpj":"11111111111111","slug":"corretora-invalida"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("IMOBILIARIA_CNPJ_INVALIDO"));
        }

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel PLATAFORMA_ADMIN retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"razaoSocial":"X","cnpj":"%s","slug":"x"}
                                    """.formatted(CnpjTestFixture.novoCnpjValido())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CNPJ duplicado retorna 409")
        void cnpjDuplicadoRetorna409() throws Exception {
            String cnpj = CnpjTestFixture.novoCnpjValido();
            String primeiroCorpo = """
                    {"razaoSocial":"Corretora Um","cnpj":"%s","slug":"corretora-um-%s"}
                    """.formatted(cnpj, System.nanoTime());
            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(primeiroCorpo))
                    .andExpect(status().isCreated());

            String segundoCorpo = """
                    {"razaoSocial":"Corretora Dois","cnpj":"%s","slug":"corretora-dois-%s"}
                    """.formatted(cnpj, System.nanoTime());
            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(segundoCorpo))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("IMOBILIARIA_CNPJ_DUPLICADO"));
        }
    }
}
