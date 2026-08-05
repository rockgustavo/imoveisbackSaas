package br.com.rockgustavo.imobiliaria.contrato;

import static br.com.rockgustavo.imobiliaria.ApiTestFixture.hojeNoFusoDoTenant;
import static br.com.rockgustavo.imobiliaria.ApiTestFixture.corpoPropriedade;
import static br.com.rockgustavo.imobiliaria.ApiTestFixture.corpoContrato;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;

class ContratoApiIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("RN-06-01/02, RN-05-05/06, RN-06-06: criação a partir de orçamento aceito")
    class Criacao {

        @Test
        @DisplayName("cria contrato em RASCUNHO copiando pessoa e itens do orçamento aceito")
        void criaContratoEmRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("rejeita orçamento que não está ACEITO")
        void rejeitaOrcamentoNaoAceito() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_ACEITO"));
        }

        @Test
        @DisplayName("RN-05-06: rejeita segundo contrato a partir do mesmo orçamento")
        void rejeitaSegundoContratoDoMesmoOrcamento() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_JA_ORIGINOU_CONTRATO"));
        }

        @Test
        @DisplayName("RN-06-01: rejeita vigência com fim anterior ou igual ao início")
        void rejeitaVigenciaInvalida() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_VIGENCIA_INVALIDA"));
        }

        @Test
        @DisplayName("RN-06-06: rejeita quando a propriedade do orçamento não pertence mais ao proprietário")
        void rejeitaPropriedadeDeOutroProprietario() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID outroProprietario = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            mockMvc.perform(put("/api/v1/propriedades/{id}", propriedadeId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(outroProprietario)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_DIVERGENTE"));
        }
    }

    @Nested
    @DisplayName("RN-06-05/07: ativação")
    class Ativacao {

        @Test
        @DisplayName("ativa contrato e move a propriedade para AGENCIADA")
        void ativaEMovePropriedadeParaAgenciada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ATIVO"));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("AGENCIADA"));
        }

        @Test
        @DisplayName("rejeita ativação com propriedade RETIRADA")
        void rejeitaAtivacaoComPropriedadeRetirada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_PROPRIEDADE_INDISPONIVEL"));
        }

        @Test
        @DisplayName("RN-06-05: rejeita ativação com vigência sobreposta a contrato ATIVO da mesma propriedade")
        void rejeitaAtivacaoComVigenciaSobreposta() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamento1 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID orcamento2 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contrato1 = fixture.criarContrato(tenantId, orcamento1,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            UUID contrato2 = fixture.criarContrato(tenantId, orcamento2,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contrato1).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contrato2).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_VIGENCIA_SOBREPOSTA"));
        }

        @Test
        @DisplayName("RN-06-05: permite ativação com vigência no ano seguinte, sem sobreposição")
        void permiteAtivacaoComVigenciaNaoSobreposta() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamento1 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID orcamento2 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contrato1 = fixture.criarContrato(tenantId, orcamento1,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            UUID contrato2 = fixture.criarContrato(tenantId, orcamento2,
                    LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contrato1).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contrato2).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ATIVO"));
        }
    }

    @Nested
    @DisplayName("RN-06-04: encerramento")
    class Encerramento {

        @Test
        @DisplayName("encerra contrato ATIVO e libera a propriedade")
        void encerraELiberaPropriedade() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"justificativa":"distrato antecipado por acordo entre as partes"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ENCERRADO"));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita encerramento sem justificativa")
        void rejeitaSemJustificativa() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"justificativa":""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PAYLOAD_INVALIDO"));
        }

        @Test
        @DisplayName("rejeita encerramento de contrato que não está ATIVO")
        void rejeitaEncerramentoForaDeAtivo() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"justificativa":"motivo qualquer"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_TRANSICAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("RN-06-10: cancelamento")
    class Cancelamento {

        @Test
        @DisplayName("cancela contrato RASCUNHO sem checagem de propriedade")
        void cancelaRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        @DisplayName("cancela contrato ATIVO sem negociação em andamento e libera a propriedade")
        void cancelaAtivoSemNegociacao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita cancelamento de ATIVO com propriedade RESERVADA")
        void rejeitaCancelamentoComPropriedadeReservada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/propriedades/{id}/reserva", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_CANCELAMENTO_INVIAVEL"));
        }
    }

    @Nested
    @DisplayName("RN-06-08/13: aditivos")
    class Aditivos {

        @Test
        @DisplayName("inclui propriedade nova em contrato ATIVO")
        void incluiPropriedadeNova() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            UUID novaPropriedade = fixture.criarPropriedade(tenantId, pessoaId);

            mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipo":"INCLUSAO","propriedadeId":"%s","justificativa":"nova propriedade",
                                     "comissaoPercentual":5.00,"valorPedido":300000.00}
                                    """.formatted(novaPropriedade)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agenciamentos.length()").value(2));
            mockMvc.perform(get("/api/v1/propriedades/{id}", novaPropriedade).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("AGENCIADA"));
        }

        @Test
        @DisplayName("renegocia comissão de propriedade já agenciada sem soltá-la")
        void renegociaSemSoltarPropriedade() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipo":"INCLUSAO","propriedadeId":"%s","justificativa":"renegociação",
                                     "comissaoPercentual":5.50,"valorPedido":500000.00}
                                    """.formatted(propriedadeId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agenciamentos.length()").value(1))
                    .andExpect(jsonPath("$.agenciamentos[0].comissaoPercentual").value("5.50"));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("AGENCIADA"));
        }

        @Test
        @DisplayName("exclui propriedade agenciada e libera a propriedade")
        void excluiPropriedadeAgenciada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipo":"EXCLUSAO","propriedadeId":"%s","justificativa":"exclusão a pedido"}
                                    """.formatted(propriedadeId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agenciamentos.length()").value(0));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("DISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita aditivo de inclusão sem comissão/valor no payload")
        void rejeitaInclusaoSemComissaoEValor() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            UUID novaPropriedade = fixture.criarPropriedade(tenantId, pessoaId);

            mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipo":"INCLUSAO","propriedadeId":"%s","justificativa":"faltam campos"}
                                    """.formatted(novaPropriedade)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PAYLOAD_INVALIDO"));
        }
    }

    @Nested
    @DisplayName("Listagem paginada e filtrável")
    class Listagem {

        @Test
        @DisplayName("lista apenas contratos do tenant corrente")
        void listaApenasDoTenantCorrente() throws Exception {
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            UUID pessoaA = fixture.criarProprietario(tenantA);
            UUID propriedadeA = fixture.criarPropriedade(tenantA, pessoaA);
            fixture.criarContrato(tenantA, fixture.criarOrcamentoAceito(tenantA, pessoaA, propriedadeA));
            UUID pessoaB = fixture.criarProprietario(tenantB);
            UUID propriedadeB = fixture.criarPropriedade(tenantB, pessoaB);
            fixture.criarContrato(tenantB, fixture.criarOrcamentoAceito(tenantB, pessoaB, propriedadeB));

            mockMvc.perform(get("/api/v1/contratos").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("filtra por status")
        void filtraPorStatus() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
            UUID contratoId = fixture.criarContrato(tenantId, orcamentoId);

            mockMvc.perform(get("/api/v1/contratos?status=RASCUNHO").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(contratoId.toString()));
            mockMvc.perform(get("/api/v1/contratos?status=ATIVO").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant por endpoint")
    class Autorizacao {

        @Nested
        @DisplayName("POST /contratos")
        class Criar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/contratos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoContrato(UUID.randomUUID(), hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoContrato(UUID.randomUUID(), hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1))))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /contratos")
        class Listar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/contratos")).andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/contratos").with(jwt())).andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /contratos/{id}")
        class Buscar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/contratos/{id}", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/contratos/{id}", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("RN-00-03: contrato de outro tenant retorna 404")
            void contratoDeOutroTenantRetorna404() throws Exception {
                UUID tenantA = fixture.criarTenant();
                UUID tenantB = fixture.criarTenant();
                UUID pessoaA = fixture.criarProprietario(tenantA);
                UUID propriedadeA = fixture.criarPropriedade(tenantA, pessoaA);
                UUID contratoId = fixture.criarContrato(tenantA,
                        fixture.criarOrcamentoAceito(tenantA, pessoaA, propriedadeA));

                mockMvc.perform(get("/api/v1/contratos/{id}", contratoId).with(administradorDoTenant(tenantB)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ENCONTRADO"));
            }
        }

        @Nested
        @DisplayName("POST /contratos/{id}/ativacao")
        class Ativar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("contrato inexistente retorna 404")
            void contratoInexistenteRetorna404() throws Exception {
                UUID tenantId = fixture.criarTenant();

                mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ENCONTRADO"));
            }
        }

        @Nested
        @DisplayName("POST /contratos/{id}/encerramento")
        class Encerrar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"justificativa":"motivo"}
                                        """))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", UUID.randomUUID())
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"justificativa":"motivo"}
                                        """))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /contratos/{id}/cancelamento")
        class Cancelar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /contratos/{id}/aditivos")
        class RegistrarAditivo {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"tipo":"EXCLUSAO","propriedadeId":"%s","justificativa":"motivo"}
                                        """.formatted(UUID.randomUUID())))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", UUID.randomUUID())
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"tipo":"EXCLUSAO","propriedadeId":"%s","justificativa":"motivo"}
                                        """.formatted(UUID.randomUUID())))
                        .andExpect(status().isForbidden());
            }
        }
    }
}
