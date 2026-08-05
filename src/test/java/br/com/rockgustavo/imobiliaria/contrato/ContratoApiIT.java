package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContratoApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("RN-06-01/02, RN-05-05/06, RN-06-06: criação a partir de orçamento aceito")
    class Criacao {

        @Test
        @DisplayName("cria contrato em RASCUNHO copiando pessoa e itens do orçamento aceito")
        void criaContratoEmRascunho() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("rejeita orçamento que não está ACEITO")
        void rejeitaOrcamentoNaoAceito() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoEObterId(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_ACEITO"));
        }

        @Test
        @DisplayName("RN-05-06: rejeita segundo contrato a partir do mesmo orçamento")
        void rejeitaSegundoContratoDoMesmoOrcamento() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_JA_ORIGINOU_CONTRATO"));
        }

        @Test
        @DisplayName("RN-06-01: rejeita vigência com fim anterior ou igual ao início")
        void rejeitaVigenciaInvalida() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, LocalDate.now(), LocalDate.now())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_VIGENCIA_INVALIDA"));
        }

        @Test
        @DisplayName("RN-06-06: rejeita quando a propriedade do orçamento não pertence mais ao proprietário")
        void rejeitaPropriedadeDeOutroProprietario() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID outroProprietario = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            mockMvc.perform(put("/api/v1/propriedades/{id}", propriedadeId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(outroProprietario)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoContrato(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1))))
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ATIVO"));
            mockMvc.perform(get("/api/v1/propriedades/{id}", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.situacao").value("AGENCIADA"));
        }

        @Test
        @DisplayName("rejeita ativação com propriedade RETIRADA")
        void rejeitaAtivacaoComPropriedadeRetirada() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_PROPRIEDADE_INDISPONIVEL"));
        }

        @Test
        @DisplayName("RN-06-05: rejeita ativação com vigência sobreposta a contrato ATIVO da mesma propriedade")
        void rejeitaAtivacaoComVigenciaSobreposta() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamento1 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID orcamento2 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contrato1 = criarContratoEObterId(tenantId, orcamento1,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            UUID contrato2 = criarContratoEObterId(tenantId, orcamento2,
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamento1 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID orcamento2 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contrato1 = criarContratoEObterId(tenantId, orcamento1,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            UUID contrato2 = criarContratoEObterId(tenantId, orcamento2,
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));

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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));

            mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        @DisplayName("cancela contrato ATIVO sem negociação em andamento e libera a propriedade")
        void cancelaAtivoSemNegociacao() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            UUID novaPropriedade = criarPropriedadeEObterId(tenantId, pessoaId);

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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
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
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
            mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            UUID novaPropriedade = criarPropriedadeEObterId(tenantId, pessoaId);

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
            UUID tenantA = criarTenant();
            UUID tenantB = criarTenant();
            UUID pessoaA = criarProprietario(tenantA);
            UUID propriedadeA = criarPropriedadeEObterId(tenantA, pessoaA);
            criarContratoEObterId(tenantA, criarOrcamentoAceitoEObterId(tenantA, pessoaA, propriedadeA),
                    LocalDate.now(), LocalDate.now().plusYears(1));
            UUID pessoaB = criarProprietario(tenantB);
            UUID propriedadeB = criarPropriedadeEObterId(tenantB, pessoaB);
            criarContratoEObterId(tenantB, criarOrcamentoAceitoEObterId(tenantB, pessoaB, propriedadeB),
                    LocalDate.now(), LocalDate.now().plusYears(1));

            mockMvc.perform(get("/api/v1/contratos").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("filtra por status")
        void filtraPorStatus() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
            UUID contratoId = criarContratoEObterId(tenantId, orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));

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
                                .content(corpoContrato(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusYears(1))))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/contratos")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoContrato(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusYears(1))))
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
                UUID tenantA = criarTenant();
                UUID tenantB = criarTenant();
                UUID pessoaA = criarProprietario(tenantA);
                UUID propriedadeA = criarPropriedadeEObterId(tenantA, pessoaA);
                UUID contratoId = criarContratoEObterId(tenantA,
                        criarOrcamentoAceitoEObterId(tenantA, pessoaA, propriedadeA),
                        LocalDate.now(), LocalDate.now().plusYears(1));

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
                UUID tenantId = criarTenant();

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

    private String corpoContrato(UUID orcamentoId, LocalDate inicio, LocalDate fim) {
        return """
                {"orcamentoId":"%s","vigenciaInicio":"%s","vigenciaFim":"%s","regrasContratuais":"regras de teste"}
                """.formatted(orcamentoId, inicio, fim);
    }

    private UUID criarContratoEObterId(UUID tenantId, UUID orcamentoId, LocalDate inicio, LocalDate fim) throws Exception {
        String location = mockMvc.perform(post("/api/v1/contratos")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoContrato(orcamentoId, inicio, fim)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private UUID criarOrcamentoAceitoEObterId(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        UUID orcamentoId = criarOrcamentoEObterId(tenantId, pessoaId, propriedadeId);
        mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        return orcamentoId;
    }

    private UUID criarOrcamentoEObterId(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        String corpo = """
                {"pessoaId":"%s","itens":[{"propriedadeId":"%s","comissaoPercentual":5.00,"valorPedido":450000.00}]}
                """.formatted(pessoaId, propriedadeId);
        String location = mockMvc.perform(post("/api/v1/orcamentos")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private String corpoPropriedade(UUID proprietarioId) {
        return """
                {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                 "valorReferencia":450000.00,"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","enderecoValidado":true}
                """.formatted(proprietarioId);
    }

    private UUID criarPropriedadeEObterId(UUID tenantId, UUID proprietarioId) throws Exception {
        String location = mockMvc.perform(post("/api/v1/propriedades")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPropriedade(proprietarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private UUID criarProprietario(UUID tenantId) throws Exception {
        String corpo = """
                {"tipoDocumento":"CPF","documento":"%s","nome":"Proprietário Teste"}
                """.formatted(CpfTestFixture.novoCpfValido());
        UUID pessoaId = criarPessoaEObterId(tenantId, corpo);

        mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"papel":"PROPRIETARIO"}
                                """))
                .andExpect(status().isOk());
        return pessoaId;
    }

    private UUID criarPessoaEObterId(UUID tenantId, String corpo) throws Exception {
        ResultActions resultado = mockMvc.perform(post("/api/v1/pessoas")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
        String location = resultado.andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
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
}
