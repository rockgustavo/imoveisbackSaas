package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.ApiTestFixture.hojeNoFusoDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Instrumento contratual em PDF")
class ContratoDocumentoIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("geração do documento")
    class Geracao {

        @Test
        @DisplayName("GET /documento sem data gera PDF do estado atual com nome de arquivo e content-type corretos")
        void geraPdfDoEstadoAtual() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);

            MvcResult resultado = mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoId)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"contrato-" + contratoId.toString().substring(0, 8) + ".pdf\""))
                    .andReturn();

            byte[] pdf = resultado.getResponse().getContentAsByteArray();
            assertThat(pdf).startsWith("%PDF".getBytes());
            String texto = extrairTexto(pdf);
            assertThat(texto).contains("ATIVO");
            assertThat(texto).doesNotContain("registro histórico");
        }

        @Test
        @DisplayName("RN-09-03: com data, usa o snapshot histórico (buscarHistoricoEm), não o estado atual, e inclui a nota de ressalva")
        void comDataUsaSnapshotHistoricoEIncluiNotaDeRessalva() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);

            MvcResult resultado = mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoId)
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andReturn();

            String texto = extrairTexto(resultado.getResponse().getContentAsByteArray());
            assertThat(texto).contains("5,00%");
            assertThat(texto).contains("registro histórico");
            assertThat(texto).contains("cadastro atual");
        }

        @Test
        @DisplayName("contrato nunca ativado não tem snapshot: 404 dedicado, mesmo padrão de /historico")
        void semSnapshotRetorna404Dedicado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoId)
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_HISTORICO_NAO_ENCONTRADO"));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant")
    class Autorizacao {

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/contratos/{id}/documento", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/contratos/{id}/documento", UUID.randomUUID()).with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RN-00-03: contrato de outro tenant retorna 404 (CONTRATO_NAO_ENCONTRADO)")
        void contratoDeOutroTenantRetorna404() throws Exception {
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            UUID pessoaA = fixture.criarProprietario(tenantA);
            UUID propriedadeA = fixture.criarPropriedade(tenantA, pessoaA);
            UUID contratoId = fixture.criarContratoAtivo(tenantA, pessoaA, propriedadeA);

            mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoId).with(administradorDoTenant(tenantB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ENCONTRADO"));
        }
    }

    static String extrairTexto(byte[] pdf) throws Exception {
        try (PDDocument documento = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }
}
