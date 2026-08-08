package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoDetalhe;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoQueryRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.ApiTestFixture.hojeNoFusoDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RN-09-03: estado do contrato em uma data de sua vigência")
class ContratoHistoricoIT extends AbstractIntegrationTest {

    @Autowired
    ContratoHistoricoQueryRepository queryRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Nested
    @DisplayName("snapshot temporal")
    class EstadoTemporal {

        @Test
        @DisplayName("snapshot de antes do aditivo mantém a comissão original; o de depois reflete a renegociação")
        void snapshotAntesEDepoisDoAditivoDivergem() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);
            Instant apósAtivacao = Instant.now();

            mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipo":"INCLUSAO","propriedadeId":"%s","justificativa":"renegociação de comissão",
                                     "comissaoPercentual":5.50,"valorPedido":500000.00}
                                    """.formatted(propriedadeId)))
                    .andExpect(status().isOk());
            Instant apósAditivo = Instant.now();

            ContratoDetalhe antes = desserializar(queryRepository.buscarEstadoEm(tenantId, contratoId, apósAtivacao).orElseThrow());
            ContratoDetalhe depois = desserializar(queryRepository.buscarEstadoEm(tenantId, contratoId, apósAditivo).orElseThrow());

            assertThat(antes.agenciamentos().get(0).comissaoPercentual()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(depois.agenciamentos().get(0).comissaoPercentual()).isEqualByComparingTo(new BigDecimal("5.50"));
        }

        @Test
        @DisplayName("GET /historico?data=hoje retorna o estado mais recente do contrato")
        void endpointRetornaEstadoMaisRecente() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(get("/api/v1/contratos/{id}/historico", contratoId)
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.versao").value(1))
                    .andExpect(jsonPath("$.contrato.status").value("ATIVO"));
        }

        @Test
        @DisplayName("contrato nunca ativado não tem snapshot: retorna 404 dedicado")
        void contratoSemSnapshotRetorna404Dedicado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(get("/api/v1/contratos/{id}/historico", contratoId)
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_HISTORICO_NAO_ENCONTRADO"));
        }

        @Test
        @DisplayName("sem o parâmetro data retorna 400 em ProblemDetail, não o erro default do container")
        void semParametroDataRetorna400EmProblemDetail() throws Exception {
            UUID tenantId = fixture.criarTenant();

            mockMvc.perform(get("/api/v1/contratos/{id}/historico", UUID.randomUUID())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PARAMETRO_OBRIGATORIO_AUSENTE"));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant")
    class Autorizacao {

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/contratos/{id}/historico", UUID.randomUUID())
                            .param("data", hojeNoFusoDoTenant().toString()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/contratos/{id}/historico", UUID.randomUUID())
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(jwt()))
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

            mockMvc.perform(get("/api/v1/contratos/{id}/historico", contratoId)
                            .param("data", hojeNoFusoDoTenant().toString())
                            .with(administradorDoTenant(tenantB)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ENCONTRADO"));
        }
    }

    private ContratoDetalhe desserializar(ContratoHistoricoView view) throws Exception {
        return objectMapper.readValue(view.snapshot(), ContratoDetalhe.class);
    }
}
