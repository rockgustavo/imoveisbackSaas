package br.com.rockgustavo.imobiliaria.orcamento;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static br.com.rockgustavo.imobiliaria.ApiTestFixture.corpoOrcamento;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrcamentoApiIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("RN-05-01/01-06: criação de orçamento")
    class Criacao {

        @Test
        @DisplayName("cria orçamento em RASCUNHO")
        void criaOrcamentoEmRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);

            mockMvc.perform(post("/api/v1/orcamentos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoOrcamento(pessoaId, propriedadeId, "5.00")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("RN-01-06: rejeita pessoa inativa")
        void rejeitaPessoaInativa() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/orcamentos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoOrcamento(pessoaId, propriedadeId, "5.00")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_INATIVA_NAO_RECEBE_ORCAMENTO"));
        }

        @Test
        @DisplayName("rejeita propriedade inexistente no tenant")
        void rejeitaPropriedadeInexistente() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);

            mockMvc.perform(post("/api/v1/orcamentos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoOrcamento(pessoaId, UUID.randomUUID(), "5.00")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_PROPRIEDADE_INDISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita propriedade repetida entre os itens")
        void rejeitaItemComPropriedadeRepetida() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            String corpo = """
                    {"pessoaId":"%s","itens":[
                        {"propriedadeId":"%s","comissaoPercentual":5.00,"valorPedido":450000.00},
                        {"propriedadeId":"%s","comissaoPercentual":4.00,"valorPedido":400000.00}
                    ]}
                    """.formatted(pessoaId, propriedadeId, propriedadeId);

            mockMvc.perform(post("/api/v1/orcamentos")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_ITEM_DUPLICADO"));
        }
    }

    @Nested
    @DisplayName("RN-05-04: atualização só em RASCUNHO")
    class Atualizacao {

        @Test
        @DisplayName("atualiza itens quando RASCUNHO")
        void atualizaItensQuandoRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            String corpo = """
                    {"itens":[{"propriedadeId":"%s","comissaoPercentual":4.00,"valorPedido":420000.00}]}
                    """.formatted(propriedadeId);

            mockMvc.perform(put("/api/v1/orcamentos/{id}", orcamentoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[0].comissaoPercentual").value("4.00"));
        }

        @Test
        @DisplayName("rejeita atualização fora de RASCUNHO")
        void rejeitaAtualizacaoForaDeRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            String corpo = """
                    {"itens":[{"propriedadeId":"%s","comissaoPercentual":4.00,"valorPedido":420000.00}]}
                    """.formatted(propriedadeId);

            mockMvc.perform(put("/api/v1/orcamentos/{id}", orcamentoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_EDITAVEL"));
        }
    }

    @Nested
    @DisplayName("RN-05-08/06-12: envio exige posse, disponibilidade e comissão dentro do teto")
    class Envio {

        @Test
        @DisplayName("envia quando propriedade disponível do proprietário do orçamento")
        void enviaQuandoValido() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");

            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ENVIADO"));
        }

        @Test
        @DisplayName("rejeita envio com propriedade de outro proprietário")
        void rejeitaEnvioComPropriedadeDeOutroProprietario() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID outroProprietario = fixture.criarProprietario(tenantId);
            UUID propriedadeDeOutro = fixture.criarPropriedade(tenantId, outroProprietario);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeDeOutro, "5.00");

            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_PROPRIEDADE_INDISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita envio com propriedade indisponível (não DISPONIVEL)")
        void rejeitaEnvioComPropriedadeIndisponivel() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", propriedadeId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_PROPRIEDADE_INDISPONIVEL"));
        }

        @Test
        @DisplayName("rejeita envio com comissão acima do teto do tenant")
        void rejeitaEnvioComComissaoAcimaDoTeto() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "50.00");

            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_COMISSAO_ACIMA_DO_TETO"));
        }
    }

    @Nested
    @DisplayName("RN-05-02/05-05: aceite e recusa")
    class AceiteERecusa {

        @Test
        @DisplayName("aceita orçamento ENVIADO")
        void aceitaQuandoEnviado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACEITO"));
        }

        @Test
        @DisplayName("recusa orçamento ENVIADO")
        void recusaQuandoEnviado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/orcamentos/{id}/recusa", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RECUSADO"));
        }

        @Test
        @DisplayName("rejeita aceitar orçamento ainda em RASCUNHO")
        void rejeitaAceitarQuandoRascunho() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");

            mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("ORCAMENTO_EXPIRADO_OU_RECUSADO"));
        }
    }

    @Nested
    @DisplayName("RN-05-07: duplicação")
    class Duplicacao {

        @Test
        @DisplayName("duplica orçamento ACEITO em nova versão RASCUNHO, sem alterar o original")
        void duplicaGeraNovaVersaoSemAlterarOriginal() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID orcamentoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            String location = mockMvc.perform(post("/api/v1/orcamentos/{id}/duplicacao", orcamentoId)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getHeader("Location");
            UUID duplicataId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

            mockMvc.perform(get("/api/v1/orcamentos/{id}", duplicataId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.status").value("RASCUNHO"))
                    .andExpect(jsonPath("$.versao").value(2))
                    .andExpect(jsonPath("$.origemId").value(orcamentoId.toString()));
            mockMvc.perform(get("/api/v1/orcamentos/{id}", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.status").value("ACEITO"));
        }
    }

    @Nested
    @DisplayName("Listagem paginada e filtrável")
    class Listagem {

        @Test
        @DisplayName("lista apenas orçamentos do tenant corrente")
        void listaApenasDoTenantCorrente() throws Exception {
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            UUID pessoaA = fixture.criarProprietario(tenantA);
            UUID propriedadeA = fixture.criarPropriedade(tenantA, pessoaA);
            fixture.criarOrcamento(tenantA, pessoaA, propriedadeA, "5.00");
            UUID pessoaB = fixture.criarProprietario(tenantB);
            UUID propriedadeB = fixture.criarPropriedade(tenantB, pessoaB);
            fixture.criarOrcamento(tenantB, pessoaB, propriedadeB, "5.00");

            mockMvc.perform(get("/api/v1/orcamentos").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("filtra por status")
        void filtraPorStatus() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            UUID rascunhoId = fixture.criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");

            mockMvc.perform(get("/api/v1/orcamentos?status=RASCUNHO").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(rascunhoId.toString()));
            mockMvc.perform(get("/api/v1/orcamentos?status=ENVIADO").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant por endpoint")
    class Autorizacao {

        @Nested
        @DisplayName("POST /orcamentos")
        class Criar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoOrcamento(UUID.randomUUID(), UUID.randomUUID(), "5.00")))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoOrcamento(UUID.randomUUID(), UUID.randomUUID(), "5.00")))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /orcamentos")
        class Listar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/orcamentos")).andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/orcamentos").with(jwt())).andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /orcamentos/{id}")
        class Buscar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/orcamentos/{id}", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/orcamentos/{id}", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("RN-00-03: orçamento de outro tenant retorna 404")
            void orcamentoDeOutroTenantRetorna404() throws Exception {
                UUID tenantA = fixture.criarTenant();
                UUID tenantB = fixture.criarTenant();
                UUID pessoaA = fixture.criarProprietario(tenantA);
                UUID propriedadeA = fixture.criarPropriedade(tenantA, pessoaA);
                UUID orcamentoId = fixture.criarOrcamento(tenantA, pessoaA, propriedadeA, "5.00");

                mockMvc.perform(get("/api/v1/orcamentos/{id}", orcamentoId).with(administradorDoTenant(tenantB)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_ENCONTRADO"));
            }
        }

        @Nested
        @DisplayName("PUT /orcamentos/{id}")
        class Atualizar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(put("/api/v1/orcamentos/{id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"itens":[{"propriedadeId":"%s","comissaoPercentual":5.00,"valorPedido":100.00}]}
                                        """.formatted(UUID.randomUUID())))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("orçamento inexistente retorna 404")
            void orcamentoInexistenteRetorna404() throws Exception {
                UUID tenantId = fixture.criarTenant();

                mockMvc.perform(put("/api/v1/orcamentos/{id}", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"itens":[{"propriedadeId":"%s","comissaoPercentual":5.00,"valorPedido":100.00}]}
                                        """.formatted(UUID.randomUUID())))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_ENCONTRADO"));
            }
        }

        @Nested
        @DisplayName("POST /orcamentos/{id}/envio")
        class Enviar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /orcamentos/{id}/aceite")
        class Aceitar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /orcamentos/{id}/recusa")
        class Recusar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/recusa", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/recusa", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /orcamentos/{id}/duplicacao")
        class Duplicar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/duplicacao", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/orcamentos/{id}/duplicacao", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("orçamento inexistente retorna 404")
            void orcamentoInexistenteRetorna404() throws Exception {
                UUID tenantId = fixture.criarTenant();

                mockMvc.perform(post("/api/v1/orcamentos/{id}/duplicacao", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("ORCAMENTO_NAO_ENCONTRADO"));
            }
        }
    }
}
