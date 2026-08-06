package br.com.rockgustavo.imobiliaria.propriedade;

import static br.com.rockgustavo.imobiliaria.ApiTestFixture.corpoPropriedade;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.usuarioDoTenant;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.geo.TestGeoConfig;

class PropriedadeApiIT extends AbstractIntegrationTest {

    @Nested
    @DisplayName("RN-03-01/02/04/10: cadastro de propriedade")
    class Cadastro {

        @Test
        @DisplayName("cria propriedade com proprietário válido")
        void criaComProprietarioValido() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(proprietarioId)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("RN-01: USUARIO também pode cadastrar")
        void usuarioTambemPodeCadastrar() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);

            mockMvc.perform(post("/api/v1/propriedades")
                            .with(usuarioDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoPropriedade(proprietarioId)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("rejeita proprietário sem papel PROPRIETARIO")
        void rejeitaProprietarioSemPapel() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaComum = fixture.criarUsuario(tenantId, "usuario@exemplo.com");

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
            UUID tenantId = fixture.criarTenant();

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
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            UUID proprietarioDoTenantB = fixture.criarProprietario(tenantB);

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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioId);

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
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            fixture.criarPropriedade(tenantA, fixture.criarProprietario(tenantA));
            fixture.criarPropriedade(tenantB, fixture.criarProprietario(tenantB));

            mockMvc.perform(get("/api/v1/propriedades").with(administradorDoTenant(tenantA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("filtra por situação")
        void filtraPorSituacao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID disponivelId = fixture.criarPropriedade(tenantId, proprietarioId);
            UUID retiradaId = fixture.criarPropriedade(tenantId, proprietarioId);
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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            fixture.criarPropriedade(tenantId, proprietarioId);

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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioId);
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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioOriginal = fixture.criarProprietario(tenantId);
            UUID novoProprietario = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioOriginal);

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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioOriginal = fixture.criarProprietario(tenantId);
            UUID novoProprietario = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioOriginal);
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
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioOriginal = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioOriginal);
            UUID pessoaComum = fixture.criarUsuario(tenantId, "outro@exemplo.com");

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAtualizacao(pessoaComum)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_PROPRIETARIO_INVALIDO"));
        }

        @Test
        @DisplayName("latitude/longitude informadas corrigem a geolocalização manualmente e marcam CONCLUIDA")
        void latitudeLongitudeCorrigemGeolocalizacaoManualmente() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID id = fixture.criarPropriedade(tenantId, proprietarioId);
            String corpo = """
                    {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                     "valorReferencia":450000.00,"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                     "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","enderecoValidado":true,
                     "latitude":-23.561684,"longitude":-46.655981}
                    """.formatted(proprietarioId);

            mockMvc.perform(put("/api/v1/propriedades/{id}", id)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.geoSituacao").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.latitude").value(-23.561684))
                    .andExpect(jsonPath("$.longitude").value(-46.655981));
        }
    }

    @Nested
    @DisplayName("POST /propriedades/geolocalizacao/pesquisar")
    class PesquisaDeGeolocalizacao {

        @Test
        @DisplayName("endereço geocodificável retorna coordenada, sem persistir nada")
        void enderecoGeocodificavelRetornaCoordenada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            String corpo = """
                    {"cep":"%s","logradouro":"Av. Paulista","numero":"1000","bairro":"Bela Vista",
                     "localidade":"São Paulo","uf":"SP"}
                    """.formatted(TestGeoConfig.CEP_GEOCODIFICAVEL);

            mockMvc.perform(post("/api/v1/propriedades/geolocalizacao/pesquisar")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encontrada").value(true))
                    .andExpect(jsonPath("$.latitude").value(-23.561684))
                    .andExpect(jsonPath("$.longitude").value(-46.655981));
        }

        @Test
        @DisplayName("endereço não geocodificável retorna encontrada=false")
        void enderecoNaoGeocodificavelRetornaNaoEncontrada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            String corpo = """
                    {"cep":"99999999","logradouro":"Rua Perdida","numero":"S/N","bairro":"Zona Rural",
                     "localidade":"Cidade","uf":"SP"}
                    """;

            mockMvc.perform(post("/api/v1/propriedades/geolocalizacao/pesquisar")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.encontrada").value(false))
                    .andExpect(jsonPath("$.latitude").doesNotExist())
                    .andExpect(jsonPath("$.longitude").doesNotExist());
        }

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(post("/api/v1/propriedades/geolocalizacao/pesquisar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                                     "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(post("/api/v1/propriedades/geolocalizacao/pesquisar")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                                     "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("RN-03-06: transições de situação expostas neste épico")
    class Transicoes {

        @Test
        @DisplayName("retira propriedade disponível")
        void retiraPropriedadeDisponivel() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID id = fixture.criarPropriedade(tenantId, fixture.criarProprietario(tenantId));

            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("RETIRADA"));
        }

        @Test
        @DisplayName("retirada é terminal — segunda retirada é rejeitada")
        void segundaRetiradaEhRejeitada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID id = fixture.criarPropriedade(tenantId, fixture.criarProprietario(tenantId));
            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/propriedades/{id}/retirada", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("reserva a partir de disponível é rejeitada — só agenciada reserva (RN-03-06)")
        void reservaAPartirDeDisponivelEhRejeitada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID id = fixture.criarPropriedade(tenantId, fixture.criarProprietario(tenantId));

            mockMvc.perform(post("/api/v1/propriedades/{id}/reserva", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("desfazer reserva a partir de disponível é rejeitado")
        void desfazerReservaAPartirDeDisponivelEhRejeitado() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID id = fixture.criarPropriedade(tenantId, fixture.criarProprietario(tenantId));

            mockMvc.perform(delete("/api/v1/propriedades/{id}/reserva", id).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PROPRIEDADE_TRANSICAO_INVALIDA"));
        }

        @Test
        @DisplayName("venda a partir de disponível é rejeitada — só reservada vende (RN-03-06/11)")
        void vendaAPartirDeDisponivelEhRejeitada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID id = fixture.criarPropriedade(tenantId, fixture.criarProprietario(tenantId));

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
                UUID tenantA = fixture.criarTenant();
                UUID tenantB = fixture.criarTenant();
                UUID id = fixture.criarPropriedade(tenantA, fixture.criarProprietario(tenantA));

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
                UUID tenantId = fixture.criarTenant();

                mockMvc.perform(put("/api/v1/propriedades/{id}", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoAtualizacao(fixture.criarProprietario(tenantId))))
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
                UUID tenantId = fixture.criarTenant();

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

    private String corpoAtualizacao(UUID proprietarioId) {
        return """
                {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                 "valorReferencia":450000.00,"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","enderecoValidado":true}
                """.formatted(proprietarioId);
    }
}
