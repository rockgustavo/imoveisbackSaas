package br.com.rockgustavo.imobiliaria.shared.auditoria;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Duration;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RN-09-01: toda transição de status vira uma linha em historico_transicao, um cenário por entidade")
class HistoricoTransicaoAssincronoIT extends AbstractIntegrationTest {

    @Autowired
    JdbcClient jdbcClient;

    @Nested
    @DisplayName("propriedade")
    class Propriedade {

        @Test
        @DisplayName("retirada gera transição DISPONIVEL → RETIRADA")
        void retiradaGeraTransicao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, proprietarioId);

            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(existeTransicao(
                    tenantId, "PROPRIEDADE", propriedadeId, "DISPONIVEL", "RETIRADA")).isTrue());
        }
    }

    @Nested
    @DisplayName("orçamento")
    class Orcamento {

        @Test
        @DisplayName("aceite gera transição ENVIADO → ACEITO")
        void aceiteGeraTransicao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, proprietarioId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, proprietarioId, propriedadeId);

            Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(existeTransicao(
                    tenantId, "ORCAMENTO", orcamentoId, "ENVIADO", "ACEITO")).isTrue());
        }
    }

    @Nested
    @DisplayName("contrato")
    class Contrato {

        @Test
        @DisplayName("ativação gera transição RASCUNHO → ATIVO")
        void ativacaoGeraTransicao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, proprietarioId);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, proprietarioId, propriedadeId);

            Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(existeTransicao(
                    tenantId, "CONTRATO", contratoId, "RASCUNHO", "ATIVO")).isTrue());
        }
    }

    private boolean existeTransicao(UUID tenantId, String entidadeTipo, UUID entidadeId, String statusAnterior,
                                     String statusNovo) {
        Long total = jdbcClient.sql("""
                SELECT count(*) FROM historico_transicao
                 WHERE tenant_id = :tenantId AND entidade_tipo = :entidadeTipo AND entidade_id = :entidadeId
                   AND status_anterior = :statusAnterior AND status_novo = :statusNovo
                """)
                .param("tenantId", tenantId)
                .param("entidadeTipo", entidadeTipo)
                .param("entidadeId", entidadeId)
                .param("statusAnterior", statusAnterior)
                .param("statusNovo", statusNovo)
                .query(Long.class)
                .single();
        return total == 1L;
    }
}
