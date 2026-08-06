package br.com.rockgustavo.imobiliaria.painel;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.ApiTestFixture.hojeNoFusoDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PainelApiIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("RN-08-01: indicadores refletem o dado real")
    class Indicadores {

        @Test
        @DisplayName("conta contratos ativos e sinaliza os que vencem em 30 dias")
        void contaContratosAtivosEVencendoEm30Dias() throws Exception {
            UUID tenantId = fixture.criarTenant();
            LocalDate hoje = hojeNoFusoDoTenant();

            UUID pessoaA = fixture.criarProprietario(tenantId);
            UUID propriedadeA = fixture.criarPropriedade(tenantId, pessoaA);
            fixture.criarContratoAtivo(tenantId, pessoaA, propriedadeA);

            UUID pessoaB = fixture.criarProprietario(tenantId);
            UUID propriedadeB = fixture.criarPropriedade(tenantId, pessoaB);
            UUID orcamentoB = fixture.criarOrcamentoAceito(tenantId, pessoaB, propriedadeB);
            UUID contratoB = fixture.criarContrato(tenantId, orcamentoB, hoje, hoje.plusDays(10));
            fixture.ativar(tenantId, contratoB);

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contratosAtivos").value(2))
                    .andExpect(jsonPath("$.contratosVencendoEm30Dias").value(1));
        }

        @Test
        @DisplayName("imóveis por situação bate com a contagem real")
        void imoveisPorSituacaoBateComContagemReal() throws Exception {
            UUID tenantId = fixture.criarTenant();

            UUID proprietario = fixture.criarProprietario(tenantId);
            fixture.criarPropriedade(tenantId, proprietario);

            UUID retiradaId = fixture.criarPropriedade(tenantId, proprietario);
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", retiradaId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            UUID agenciadaId = fixture.criarPropriedade(tenantId, proprietario);
            fixture.criarContratoAtivo(tenantId, proprietario, agenciadaId);

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imoveisPorSituacao.DISPONIVEL").value(1))
                    .andExpect(jsonPath("$.imoveisPorSituacao.RETIRADA").value(1))
                    .andExpect(jsonPath("$.imoveisPorSituacao.AGENCIADA").value(1))
                    .andExpect(jsonPath("$.imoveisPorSituacao.RESERVADA").value(0))
                    .andExpect(jsonPath("$.imoveisPorSituacao.VENDIDA").value(0));
        }

        @Test
        @DisplayName("orçamentos aguardando resposta conta ENVIADO dentro da validade e ignora RECUSADO")
        void orcamentosAguardandoRespostaIgnoraRecusado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoa = fixture.criarProprietario(tenantId);

            UUID propriedadeAguardando = fixture.criarPropriedade(tenantId, pessoa);
            UUID orcamentoAguardando = fixture.criarOrcamento(tenantId, pessoa, propriedadeAguardando);
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoAguardando).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            UUID propriedadeRecusada = fixture.criarPropriedade(tenantId, pessoa);
            UUID orcamentoRecusado = fixture.criarOrcamento(tenantId, pessoa, propriedadeRecusada);
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoRecusado).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/orcamentos/{id}/recusa", orcamentoRecusado).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orcamentosAguardandoResposta").value(1));
        }

        @Test
        @DisplayName("funil bate com a contagem real por classificação (RN-01-09)")
        void funilBateComContagemReal() throws Exception {
            UUID tenantId = fixture.criarTenant();

            fixture.criarPessoa(tenantId, CpfTestFixture.novoCpfValido(),
                    "Lead");

            UUID pessoaProspect = fixture.criarProprietario(tenantId);
            UUID propriedadeProspect = fixture.criarPropriedade(tenantId, pessoaProspect);
            UUID orcamentoProspect = fixture.criarOrcamento(tenantId, pessoaProspect, propriedadeProspect);
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoProspect).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            UUID pessoaCliente = fixture.criarProprietario(tenantId);
            fixture.criarContratoAtivo(tenantId, pessoaCliente, fixture.criarPropriedade(tenantId, pessoaCliente));

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.funil.lead").value(1))
                    .andExpect(jsonPath("$.funil.prospect").value(1))
                    .andExpect(jsonPath("$.funil.cliente").value(1))
                    .andExpect(jsonPath("$.funil.clienteInativo").value(0));
        }
    }

    @Nested
    @DisplayName("RN-08-02: isolamento de tenant")
    class IsolamentoDeTenant {

        @Test
        @DisplayName("indicadores nunca misturam dado de outro tenant")
        void indicadoresNaoMisturamOutroTenant() throws Exception {
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();

            UUID pessoaB = fixture.criarProprietario(tenantB);
            fixture.criarContratoAtivo(tenantB, pessoaB, fixture.criarPropriedade(tenantB, pessoaB));

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contratosAtivos").value(0))
                    .andExpect(jsonPath("$.comissaoProjetada").value("0.00"));
        }
    }

    @Nested
    @DisplayName("RN-08-03: comissão projetada")
    class ComissaoProjetada {

        @Test
        @DisplayName("soma comissão de agenciamento com contrato ATIVO e vigência corrente")
        void somaComissaoDeAgenciamentoVigente() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoa = fixture.criarProprietario(tenantId);
            UUID propriedade = fixture.criarPropriedade(tenantId, pessoa);
            UUID orcamento = fixture.criarOrcamento(tenantId, pessoa, propriedade, "6.00");
            fixture.aceitar(tenantId, orcamento);
            UUID contrato = fixture.criarContrato(tenantId, orcamento);
            fixture.ativar(tenantId, contrato);

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comissaoProjetada").value("27000.00"));
        }

        @Test
        @DisplayName("agenciamento de contrato com vigência totalmente no passado não entra na soma")
        void agenciamentoComVigenciaNoPassadoNaoEntraNaSoma() throws Exception {
            UUID tenantId = fixture.criarTenant();
            LocalDate hoje = hojeNoFusoDoTenant();
            UUID pessoa = fixture.criarProprietario(tenantId);
            UUID propriedade = fixture.criarPropriedade(tenantId, pessoa);
            UUID orcamento = fixture.criarOrcamento(tenantId, pessoa, propriedade, "6.00");
            fixture.aceitar(tenantId, orcamento);
            UUID contrato = fixture.criarContrato(tenantId, orcamento, hoje.minusYears(2), hoje.minusDays(1));
            fixture.ativar(tenantId, contrato);

            mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comissaoProjetada").value("0.00"));
        }
    }

    @Nested
    @DisplayName("GET /painel/indicadores — autenticação e autorização")
    class Autorizacao {

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/painel/indicadores"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/painel/indicadores").with(jwt()))
                    .andExpect(status().isForbidden());
        }
    }
}
