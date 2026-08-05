package br.com.rockgustavo.imobiliaria.shared.security;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.comoPessoa;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.pessoa.infra.TestKeycloakAdminConfig;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;

class AcessoAtivoApiIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("RN-02-04/ADR-08: revogação imediata de acesso")
    class RevogacaoImediata {

        @Test
        @DisplayName("pessoa inativada perde acesso na requisição seguinte, mesmo com token ainda válido")
        void pessoaInativadaPerdeAcessoNaProximaRequisicao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            String subjectAtor = criarAdministrador(tenantId, "Administradora Ativa", "atora@exemplo.com").subjectIdp();
            Administrador alvo = criarAdministrador(tenantId, "Administrador Alvo", "alvo@exemplo.com");

            mockMvc.perform(get("/api/v1/pessoas").with(comoPessoa(tenantId, alvo.subjectIdp())))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", alvo.id())
                            .with(comoPessoa(tenantId, subjectAtor)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/pessoas").with(comoPessoa(tenantId, alvo.subjectIdp())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ACESSO_REVOGADO"));
        }

        @Test
        @DisplayName("subject sem pessoa vinculada (ex.: PLATAFORMA_ADMIN) não é bloqueado")
        void subjectSemPessoaVinculadaNaoEBloqueado() throws Exception {
            String corpo = """
                    {"razaoSocial":"Corretora Exemplo","cnpj":"%s","slug":"corretora-%s"}
                    """.formatted(CnpjTestFixture.novoCnpjValido(), System.nanoTime());

            mockMvc.perform(post("/api/v1/plataforma/imobiliarias")
                            .with(jwt().authorities(() -> "ROLE_PLATAFORMA_ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isCreated());
        }
    }

    private Administrador criarAdministrador(UUID tenantId, String nome, String email) throws Exception {
        String corpoPessoa = """
                {"tipoDocumento":"CPF","documento":"%s","nome":"%s"}
                """.formatted(CpfTestFixture.novoCpfValido(), nome);
        String location = mockMvc.perform(post("/api/v1/pessoas")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPessoa))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        UUID pessoaId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

        mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"papel":"ADMINISTRADOR","email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());
        return new Administrador(pessoaId, TestKeycloakAdminConfig.subjectIdpDeterministico(email, nome, tenantId));
    }

    private record Administrador(UUID id, String subjectIdp) {
    }
}
