package br.com.rockgustavo.imobiliaria.propriedade;

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

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropriedadeApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("RN-03-01/02/04/10: cadastro de propriedade")
    class Cadastro {

        @Test
        @DisplayName("cria propriedade com proprietário válido")
        void criaComProprietarioValido() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(proprietarioId)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("RN-01: USUARIO também pode cadastrar")
        void usuarioTambemPodeCadastrar() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(usuarioDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(proprietarioId)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("rejeita proprietário sem papel PROPRIETARIO")
        void rejeitaProprietarioSemPapel() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaComum = criarUsuarioComum(tenantId, "usuario@exemplo.com");

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(pessoaComum)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_INVALIDO"));
        }

        @Test
        @DisplayName("rejeita proprietário inexistente")
        void rejeitaProprietarioInexistente() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(UUID.randomUUID())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_INVALIDO"));
        }

        @Test
        @DisplayName("rejeita proprietário de outro tenant")
        void rejeitaProprietarioDeOutroTenant() throws Exception {
            UUID tenantA = criarTenant();
            UUID tenantB = criarTenant();
            UUID proprietarioDoTenantB = criarProprietario(tenantB);

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(proprietarioDoTenantB)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_INVALIDO"));
        }

        @Test
        @DisplayName("RN-03-04: aceita endereço não validado, marcado explicitamente pelo chamador")
        void aceitaEnderecoNaoValidado() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);
            String corpo = """
                    {"proprietarioId":"%s","tipo":"CASA","valorReferencia":300000.00,
                     "cep":"99999999","logradouro":"Preenchido manualmente","numero":"S/N",
                     "bairro":"Zona Rural","localidade":"Cidade","uf":"SP","enderecoValidado":false}
                    """.formatted(proprietarioId);

            String location = mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getHeader("Location");
            UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

            mockMvc.perform(get("/api/v1/propriedades/{id}", id).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.enderecoValidado").value(false));
        }

        @Test
        @DisplayName("RN-03-02: endereço persiste como snapshot, sem depender de reconsulta")
        void enderecoPersisteComoSnapshot() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);
            UUID id = criarPropriedadeEObterId(tenantId, proprietarioId);

            mockMvc.perform(get("/api/v1/propriedades/{id}", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.logradouro").value("Av. Paulista"))
                    .andExpect(jsonPath("$.bairro").value("Bela Vista"))
                    .andExpect(jsonPath("$.localidade").value("São Paulo"))
                    .andExpect(jsonPath("$.uf").value("SP"))
                    .andExpect(jsonPath("$.situacao").value("DISPONIVEL"))
                    .andExpect(jsonPath("$.geoSituacao").value("PENDENTE"));
        }
    }

    @Nested
    @DisplayName("Listagem paginada e filtrável")
    class Listagem {

        @Test
        @DisplayName("lista apenas propriedades do tenant corrente")
        void listaApenasDoTenantCorrente() throws Exception {
            UUID tenantA = criarTenant();
            UUID tenantB = criarTenant();
            criarPropriedadeEObterId(tenantA, criarProprietario(tenantA));
            criarPropriedadeEObterId(tenantB, criarProprietario(tenantB));

            mockMvc.perform(get("/api/v1/propriedades").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("filtra por situação")
        void filtraPorSituacao() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);
            UUID disponivelId = criarPropriedadeEObterId(tenantId, proprietarioId);
            UUID retiradaId = criarPropriedadeEObterId(tenantId, proprietarioId);
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", retiradaId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/propriedades?situacao=RETIRADA").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(retiradaId.toString()));

            mockMvc.perform(get("/api/v1/propriedades?situacao=DISPONIVEL").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.content[0].id").value(disponivelId.toString()));
        }

        @Test
        @DisplayName("filtra por faixa de valor de referência")
        void filtraPorFaixaDeValor() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);
            criarPropriedadeEObterId(tenantId, proprietarioId);

            mockMvc.perform(get("/api/v1/propriedades?valorMin=1000000").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(0));

            mockMvc.perform(get("/api/v1/propriedades?valorMax=1000000").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("RN-03-07: atualização e troca de proprietário")
    class Atualizacao {

        @Test
        @DisplayName("atualiza dados cadastrais")
        void atualizaDadosCadastrais() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioId = criarProprietario(tenantId);
            UUID id = criarPropriedadeEObterId(tenantId, proprietarioId);
            String corpo = """
                    {"proprietarioId":"%s","tipo":"CASA","areaPrivativa":150.00,"quartos":4,"vagas":2,
                     "valorReferencia":700000.00,"cep":"20040020","logradouro":"Av. Rio Branco","numero":"1",
                     "bairro":"Centro","localidade":"Rio de Janeiro","uf":"RJ","enderecoValidado":true}
                    """.formatted(proprietarioId);

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tipo").value("CASA"))
                    .andExpect(jsonPath("$.localidade").value("Rio de Janeiro"));
        }

        @Test
        @DisplayName("permite trocar proprietário quando disponível")
        void permiteTrocaDeProprietarioQuandoDisponivel() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioOriginal = criarProprietario(tenantId);
            UUID novoProprietario = criarProprietario(tenantId);
            UUID id = criarPropriedadeEObterId(tenantId, proprietarioOriginal);

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAtualizacao(novoProprietario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proprietarioId").value(novoProprietario.toString()));
        }

        @Test
        @DisplayName("permite trocar proprietário quando retirada")
        void permiteTrocaDeProprietarioQuandoRetirada() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioOriginal = criarProprietario(tenantId);
            UUID novoProprietario = criarProprietario(tenantId);
            UUID id = criarPropriedadeEObterId(tenantId, proprietarioOriginal);
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAtualizacao(novoProprietario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proprietarioId").value(novoProprietario.toString()));
        }

        @Test
        @DisplayName("rejeita novo proprietário inválido")
        void rejeitaNovoProprietarioInvalido() throws Exception {
            UUID tenantId = criarTenant();
            UUID proprietarioOriginal = criarProprietario(tenantId);
            UUID id = criarPropriedadeEObterId(tenantId, proprietarioOriginal);
            UUID pessoaComum = criarUsuarioComum(tenantId, "outro@exemplo.com");

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAtualizacao(pessoaComum)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_INVALIDO"));
        }
    }

    @Nested
    @DisplayName("RN-03-06: transições de situação expostas neste épico")
    class Transicoes {

        @Test
        @DisplayName("retira propriedade disponível")
        void retiraPropriedadeDisponivel() throws Exception {
            UUID tenantId = criarTenant();
            UUID id = criarPropriedadeEObterId(tenantId, criarProprietario(tenantId));

            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("RETIRADA"));
        }

        @Test
        @DisplayName("retirada é terminal — segunda retirada é rejeitada")
        void segundaRetiradaEhRejeitada() throws Exception {
            UUID tenantId = criarTenant();
            UUID id = criarPropriedadeEObterId(tenantId, criarProprietario(tenantId));
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("reserva a partir de disponível é rejeitada — só agenciada reserva (RN-03-06)")
        void reservaAPartirDeDisponivelEhRejeitada() throws Exception {
            UUID tenantId = criarTenant();
            UUID id = criarPropriedadeEObterId(tenantId, criarProprietario(tenantId));

            mockMvc.perform(post("/api/v1/propriedades/{id}/reserva", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("desfazer reserva a partir de disponível é rejeitado")
        void desfazerReservaAPartirDeDisponivelEhRejeitado() throws Exception {
            UUID tenantId = criarTenant();
            UUID id = criarPropriedadeEObterId(tenantId, criarProprietario(tenantId));

            mockMvc.perform(delete("/api/v1/propriedades/{id}/reserva", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("venda a partir de disponível é rejeitada — só reservada vende (RN-03-06/11)")
        void vendaAPartirDeDisponivelEhRejeitada() throws Exception {
            UUID tenantId = criarTenant();
            UUID id = criarPropriedadeEObterId(tenantId, criarProprietario(tenantId));

            mockMvc.perform(post("/api/v1/propriedades/{id}/venda", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant por endpoint")
    class Autorizacao {

        @Nested
        @DisplayName("POST /propriedades")
        class Criar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoPropriedade(UUID.randomUUID())))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoPropriedade(UUID.randomUUID())))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /propriedades")
        class Listar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/propriedades")).andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/propriedades").with(jwt())).andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /propriedades/{id}")
        class Buscar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/propriedades/{id}", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/propriedades/{id}", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("propriedade de outro tenant retorna 404")
            void propriedadeDeOutroTenantRetorna404() throws Exception {
                UUID tenantA = criarTenant();
                UUID tenantB = criarTenant();
                UUID id = criarPropriedadeEObterId(tenantA, criarProprietario(tenantA));

                mockMvc.perform(get("/api/v1/propriedades/{id}", id).with(administradorDoTenant(tenantB)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("PUT /propriedades/{id}")
        class Atualizar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(put("/api/v1/propriedades/{id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoAtualizacao(UUID.randomUUID())))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(put("/api/v1/propriedades/{id}", UUID.randomUUID())
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoAtualizacao(UUID.randomUUID())))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("propriedade inexistente retorna 404")
            void propriedadeInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(put("/api/v1/propriedades/{id}", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoAtualizacao(criarProprietario(tenantId))))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("POST /propriedades/{id}/retirada")
        class Retirar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("propriedade inexistente retorna 404")
            void propriedadeInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("POST /propriedades/{id}/reserva")
        class Reservar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/reserva", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/reserva", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("DELETE /propriedades/{id}/reserva")
        class DesfazerReserva {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(delete("/api/v1/propriedades/{id}/reserva", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(delete("/api/v1/propriedades/{id}/reserva", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("POST /propriedades/{id}/venda")
        class Vender {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/venda", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/propriedades/{id}/venda", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }
    }

    private String corpoPropriedade(UUID proprietarioId) {
        return """
                {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                 "valorReferencia":450000.00,"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","enderecoValidado":true}
                """.formatted(proprietarioId);
    }

    private String corpoAtualizacao(UUID proprietarioId) {
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

    private UUID criarUsuarioComum(UUID tenantId, String email) throws Exception {
        String corpo = """
                {"tipoDocumento":"CPF","documento":"%s","nome":"Usuário Comum"}
                """.formatted(CpfTestFixture.novoCpfValido());
        UUID pessoaId = criarPessoaEObterId(tenantId, corpo);

        mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"papel":"USUARIO","email":"%s"}
                                """.formatted(email)))
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

    private static RequestPostProcessor usuarioDoTenant(UUID tenantId) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_USUARIO");
    }
}
