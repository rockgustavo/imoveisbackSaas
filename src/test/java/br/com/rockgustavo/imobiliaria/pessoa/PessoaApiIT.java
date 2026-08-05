package br.com.rockgustavo.imobiliaria.pessoa;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.pessoa.infra.TestKeycloakAdminConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PessoaApiIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("RN-01-02: documento único por tenant")
    class DocumentoUnicoPorTenant {

        @Test
        @DisplayName("documento duplicado no mesmo tenant é rejeitado")
        void documentoDuplicadoNoMesmoTenantRejeitado() throws Exception {
            UUID tenantId = criarTenant();
            String cpf = CpfTestFixture.novoCpfValido();

            criarPessoa(tenantId, cpf, "Fulano de Tal").andExpect(status().isCreated());

            criarPessoa(tenantId, cpf, "Outro Nome")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_DOCUMENTO_DUPLICADO"));
        }

        @Test
        @DisplayName("mesmo documento em tenants diferentes é aceito")
        void mesmoDocumentoEmTenantsDiferentesAceito() throws Exception {
            UUID tenantA = criarTenant();
            UUID tenantB = criarTenant();
            String cpf = CpfTestFixture.novoCpfValido();

            criarPessoa(tenantA, cpf, "Fulano de Tal").andExpect(status().isCreated());
            criarPessoa(tenantB, cpf, "Fulano de Tal").andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("RN-01-07: último administrador ativo")
    class UltimoAdministrador {

        @Test
        @DisplayName("remover papel ADMINISTRADOR do único administrador é rejeitado")
        void removerPapelDoUnicoAdministradorRejeitado() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Admin Único");
            atribuirPapel(tenantId, pessoaId, "ADMINISTRADOR", "admin1@exemplo.com").andExpect(status().isOk());

            mockMvc.perform(delete("/api/v1/pessoas/{id}/papeis/ADMINISTRADOR", pessoaId)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_ULTIMO_ADMINISTRADOR"));
        }

        @Test
        @DisplayName("inativar o único administrador ativo é rejeitado")
        void inativarUnicoAdministradorRejeitado() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Admin Único");
            atribuirPapel(tenantId, pessoaId, "ADMINISTRADOR", "admin2@exemplo.com").andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", pessoaId)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_ULTIMO_ADMINISTRADOR"));
        }
    }

    @Nested
    @DisplayName("RN-01-09: classificação comercial")
    class Classificacao {

        @Test
        @DisplayName("pessoa sem orçamento e sem contrato retorna LEAD")
        void pessoaSemOrcamentoSemContratoRetornaLead() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Fulano de Tal");

            mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classificacao").value("LEAD"));
        }

        @Test
        @DisplayName("pessoa com orçamento ENVIADO e validade futura retorna PROSPECT")
        void pessoaComOrcamentoEnviadoRetornaProspect() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID orcamentoId = criarOrcamentoEObterId(tenantId, pessoaId, propriedadeId);
            mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.classificacao").value("PROSPECT"));
        }

        @Test
        @DisplayName("pessoa com contrato ATIVO retorna CLIENTE")
        void pessoaComContratoAtivoRetornaCliente() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            criarContratoAtivoEObterId(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.classificacao").value("CLIENTE"));
        }

        @Test
        @DisplayName("pessoa cujo único contrato foi ENCERRADO retorna CLIENTE_INATIVO")
        void pessoaComContratoEncerradoRetornaClienteInativo() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID contratoId = criarContratoAtivoEObterId(tenantId, pessoaId, propriedadeId);
            mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", contratoId)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"justificativa":"distrato antecipado por acordo entre as partes"}
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.classificacao").value("CLIENTE_INATIVO"));
        }

        @Test
        @DisplayName("pessoa com um contrato ATIVO e outro ENCERRADO retorna CLIENTE — precedência sobre CLIENTE_INATIVO")
        void pessoaComContratoAtivoEEncerradoRetornaCliente() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarProprietario(tenantId);
            UUID propriedade1 = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID propriedade2 = criarPropriedadeEObterId(tenantId, pessoaId);
            UUID contrato1 = criarContratoAtivoEObterId(tenantId, pessoaId, propriedade1);
            mockMvc.perform(post("/api/v1/contratos/{id}/encerramento", contrato1)
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"justificativa":"distrato antecipado por acordo entre as partes"}
                                    """))
                    .andExpect(status().isOk());
            criarContratoAtivoEObterId(tenantId, pessoaId, propriedade2);

            mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.classificacao").value("CLIENTE"));
        }

        @Test
        @DisplayName("filtro ?classificacao= retorna só pessoas na classificação pedida")
        void filtraPorClassificacao() throws Exception {
            UUID tenantId = criarTenant();
            UUID leadId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Lead Puro");
            UUID clienteId = criarProprietario(tenantId);
            UUID propriedadeId = criarPropriedadeEObterId(tenantId, clienteId);
            criarContratoAtivoEObterId(tenantId, clienteId, propriedadeId);

            mockMvc.perform(get("/api/v1/pessoas?classificacao=CLIENTE").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(clienteId.toString()));
            mockMvc.perform(get("/api/v1/pessoas?classificacao=LEAD").with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(leadId.toString()));
        }
    }

    @Nested
    @DisplayName("RN-02-03: usuário não altera os próprios papéis")
    class AutoAlteracaoDePapel {

        @Test
        @DisplayName("administrador não pode atribuir papel a si mesmo")
        void administradorNaoPodeAtribuirPapelASiMesmo() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Fulano de Tal");
            atribuirPapel(tenantId, pessoaId, "ADMINISTRADOR", "fulano@exemplo.com").andExpect(status().isOk());
            String subject = TestKeycloakAdminConfig.subjectIdpDeterministico("fulano@exemplo.com", "Fulano de Tal", tenantId);

            mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                            .with(comoPessoa(tenantId, subject))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"papel":"USUARIO","email":"fulano@exemplo.com"}
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_PAPEL_PROPRIO_IMUTAVEL"));
        }

        @Test
        @DisplayName("administrador não pode remover o próprio papel")
        void administradorNaoPodeRemoverOProprioPapel() throws Exception {
            UUID tenantId = criarTenant();
            UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Fulano de Tal");
            atribuirPapel(tenantId, pessoaId, "ADMINISTRADOR", "fulano@exemplo.com").andExpect(status().isOk());
            String subject = TestKeycloakAdminConfig.subjectIdpDeterministico("fulano@exemplo.com", "Fulano de Tal", tenantId);

            mockMvc.perform(delete("/api/v1/pessoas/{id}/papeis/ADMINISTRADOR", pessoaId)
                            .with(comoPessoa(tenantId, subject)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("PESSOA_PAPEL_PROPRIO_IMUTAVEL"));
        }

        @Test
        @DisplayName("administrador pode atribuir papel a outra pessoa")
        void administradorPodeAtribuirPapelAOutraPessoa() throws Exception {
            UUID tenantId = criarTenant();
            UUID autorId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Autora");
            atribuirPapel(tenantId, autorId, "ADMINISTRADOR", "autora@exemplo.com").andExpect(status().isOk());
            String subjectAutora = TestKeycloakAdminConfig.subjectIdpDeterministico("autora@exemplo.com", "Autora", tenantId);
            UUID alvoId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Alvo");

            mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", alvoId)
                            .with(comoPessoa(tenantId, subjectAutora))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"papel":"PROPRIETARIO"}
                                    """))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Campo obrigatório ausente volta identificado, não como alerta genérico")
    class CamposObrigatorios {

        @Test
        @DisplayName("payload sem nome e sem documento nomeia os dois campos em campos{}")
        void payloadIncompletoNomeiaOsCampos() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(post("/api/v1/pessoas")
                            .with(administradorDoTenant(tenantId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipoDocumento":"CPF","documento":"","nome":""}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PAYLOAD_INVALIDO"))
                    .andExpect(jsonPath("$.campos.documento").value("Campo obrigatório"))
                    .andExpect(jsonPath("$.campos.nome").value("Campo obrigatório"));
        }

        @Test
        @DisplayName("mensagem do campo não depende do Accept-Language do cliente")
        void mensagemNaoDependeDoIdiomaDoCliente() throws Exception {
            UUID tenantId = criarTenant();

            mockMvc.perform(post("/api/v1/pessoas")
                            .with(administradorDoTenant(tenantId))
                            .header("Accept-Language", "en-US")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipoDocumento":"CPF","documento":"%s","nome":"Fulano de Tal","email":"nao-e-email"}
                                    """.formatted(CpfTestFixture.novoCpfValido())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.email").value("E-mail inválido"));
        }
    }

    @Nested
    @DisplayName("Autorização, autenticação e isolamento de tenant por endpoint")
    class Autorizacao {

        @Nested
        @DisplayName("POST /pessoas")
        class Criar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoCriarPessoa(CpfTestFixture.novoCpfValido())))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas")
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpoCriarPessoa(CpfTestFixture.novoCpfValido())))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /pessoas")
        class Listar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/pessoas"))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/pessoas").with(jwt()))
                        .andExpect(status().isForbidden());
            }
        }

        @Nested
        @DisplayName("GET /pessoas/{id}")
        class Buscar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(get("/api/v1/pessoas/{id}", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(get("/api/v1/pessoas/{id}", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("pessoa de outro tenant retorna 404")
            void pessoaDeOutroTenantRetorna404() throws Exception {
                UUID tenantA = criarTenant();
                UUID tenantB = criarTenant();
                UUID pessoaId = criarPessoaEObterId(tenantA, CpfTestFixture.novoCpfValido(), "Fulano de Tal");

                mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaId).with(administradorDoTenant(tenantB)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("PUT /pessoas/{id}")
        class Atualizar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(put("/api/v1/pessoas/{id}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"nome":"Novo Nome"}
                                        """))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(put("/api/v1/pessoas/{id}", UUID.randomUUID())
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"nome":"Novo Nome"}
                                        """))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("pessoa inexistente retorna 404")
            void pessoaInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(put("/api/v1/pessoas/{id}", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"nome":"Novo Nome"}
                                        """))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("POST /pessoas/{id}/papeis")
        class AtribuirPapel {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"papel":"USUARIO","email":"fulano@exemplo.com"}
                                        """))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", UUID.randomUUID())
                                .with(jwt())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"papel":"USUARIO","email":"fulano@exemplo.com"}
                                        """))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("pessoa inexistente retorna 404")
            void pessoaInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"papel":"USUARIO","email":"fulano@exemplo.com"}
                                        """))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("DELETE /pessoas/{id}/papeis/{papel}")
        class RemoverPapel {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(delete("/api/v1/pessoas/{id}/papeis/ADMINISTRADOR", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(delete("/api/v1/pessoas/{id}/papeis/ADMINISTRADOR", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("pessoa inexistente retorna 404")
            void pessoaInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(delete("/api/v1/pessoas/{id}/papeis/ADMINISTRADOR", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"));
            }
        }

        @Nested
        @DisplayName("POST /pessoas/{id}/inativacao")
        class Inativar {

            @Test
            @DisplayName("sem token retorna 401")
            void semTokenRetorna401() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", UUID.randomUUID()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("sem papel ADMINISTRADOR retorna 403")
            void semPapelRetorna403() throws Exception {
                mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", UUID.randomUUID()).with(jwt()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("pessoa inexistente retorna 404")
            void pessoaInexistenteRetorna404() throws Exception {
                UUID tenantId = criarTenant();

                mockMvc.perform(post("/api/v1/pessoas/{id}/inativacao", UUID.randomUUID())
                                .with(administradorDoTenant(tenantId)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"));
            }
        }
    }

    private static String corpoCriarPessoa(String documento) {
        return """
                {"tipoDocumento":"CPF","documento":"%s","nome":"Fulano de Tal"}
                """.formatted(documento);
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

    private ResultActions criarPessoa(UUID tenantId, String documento, String nome) throws Exception {
        String corpo = """
                {"tipoDocumento":"CPF","documento":"%s","nome":"%s"}
                """.formatted(documento, nome);
        return mockMvc.perform(post("/api/v1/pessoas")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private UUID criarPessoaEObterId(UUID tenantId, String documento, String nome) throws Exception {
        String location = criarPessoa(tenantId, documento, nome)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private ResultActions atribuirPapel(UUID tenantId, UUID pessoaId, String papel, String email) throws Exception {
        String corpo = """
                {"papel":"%s","email":"%s"}
                """.formatted(papel, email);
        return mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private UUID criarProprietario(UUID tenantId) throws Exception {
        UUID pessoaId = criarPessoaEObterId(tenantId, CpfTestFixture.novoCpfValido(), "Proprietário Teste");
        mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"papel":"PROPRIETARIO"}
                                """))
                .andExpect(status().isOk());
        return pessoaId;
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

    private UUID criarContratoAtivoEObterId(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        UUID orcamentoId = criarOrcamentoEObterId(tenantId, pessoaId, propriedadeId);
        mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        String corpoContrato = """
                {"orcamentoId":"%s","vigenciaInicio":"%s","vigenciaFim":"%s","regrasContratuais":"regras de teste"}
                """.formatted(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
        String location = mockMvc.perform(post("/api/v1/contratos")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoContrato))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        UUID contratoId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        return contratoId;
    }

    private static RequestPostProcessor administradorDoTenant(UUID tenantId) {
        return jwt()
                .jwt(builder -> builder.claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor comoPessoa(UUID tenantId, String subjectIdp) {
        return jwt()
                .jwt(builder -> builder.subject(subjectIdp).claim("tenant_id", tenantId.toString()))
                .authorities(() -> "ROLE_ADMINISTRADOR");
    }
}
