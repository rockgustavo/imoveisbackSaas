package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Endereco;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeTestBuilder;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MapaApiIT extends AbstractIntegrationTest {

    private static final String BBOX_SAO_PAULO = "-23.60,-46.70,-23.50,-46.60";
    private static final BigDecimal LAT_DENTRO = new BigDecimal("-23.55");
    private static final BigDecimal LON_DENTRO = new BigDecimal("-46.65");
    private static final BigDecimal LAT_FORA = new BigDecimal("-22.90");
    private static final BigDecimal LON_FORA = new BigDecimal("-43.20");

    @Autowired
    PropriedadeRepository propriedadeRepository;

    MapaTestFixture mapaFixture;

    @BeforeEach
    void configurarFixture() {
        mapaFixture = new MapaTestFixture(propriedadeRepository);
    }

    @Nested
    @DisplayName("RN-07-01: só propriedades geocodificadas aparecem")
    class Geocodificacao {

        @Test
        @DisplayName("propriedade pendente de geocodificação nunca aparece no mapa")
        void propriedadePendenteNuncaAparece() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            fixture.criarPropriedade(tenantId, proprietarioId);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(1));
        }

        @Test
        @DisplayName("bounding box exclui propriedade geocodificada fora da área visível")
        void bboxExcluiPropriedadeForaDaArea() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_FORA, LON_FORA);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(1));
        }
    }

    @Nested
    @DisplayName("RN-07-02: filtros combináveis")
    class Filtros {

        @Test
        @DisplayName("filtra por situação explícita do imóvel")
        void filtraPorSituacao() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID retiradaId = mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO,
                    SituacaoPropriedade.RETIRADA);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.AGENCIADA);

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("situacao", "RETIRADA")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(retiradaId.toString()));
        }

        @Test
        @DisplayName("filtra por faixa de valor de referência")
        void filtraPorFaixaDeValor() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.AGENCIADA,
                    PropriedadeTestBuilder.enderecoPadrao(), new BigDecimal("450000.00"));

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("valorMin", "1000000")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(0));

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("valorMax", "1000000")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1));
        }

        @Test
        @DisplayName("filtra por cidade e UF")
        void filtraPorLocalidadeEUf() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            Endereco rio = new Endereco("20040020", "Av. Rio Branco", "1", null, "Centro", "Rio de Janeiro", "RJ", true);
            UUID doRioId = mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO,
                    SituacaoPropriedade.AGENCIADA, rio, new BigDecimal("450000.00"));
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.AGENCIADA);

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("localidade", "Rio de Janeiro")
                            .param("uf", "RJ")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(doRioId.toString()));
        }

        @Test
        @DisplayName("filtra por proprietário")
        void filtraPorProprietario() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioA = fixture.criarProprietario(tenantId);
            UUID proprietarioB = fixture.criarProprietario(tenantId);
            mapaFixture.criarGeocodificada(tenantId, proprietarioA, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.AGENCIADA);
            UUID doProprietarioBId = mapaFixture.criarGeocodificada(tenantId, proprietarioB, LAT_DENTRO, LON_DENTRO,
                    SituacaoPropriedade.AGENCIADA);

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("proprietarioId", proprietarioB.toString())
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(doProprietarioBId.toString()));
        }

        @Test
        @DisplayName("propriedade sem nenhum contrato não aparece quando statusContrato é informado")
        void statusContratoExcluiPropriedadeSemContrato() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.AGENCIADA);

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("statusContrato", "ATIVO")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(0));
        }

        @Test
        @DisplayName("statusContrato=ATIVO encontra propriedade com contrato corrente ativo")
        void statusContratoEncontraContratoAtivo() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            mapaFixture.geocodificarExistente(tenantId, propriedadeId, LAT_DENTRO, LON_DENTRO);
            fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("statusContrato", "ATIVO")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(propriedadeId.toString()))
                    .andExpect(jsonPath("$.propriedades[0].statusContrato").value("ATIVO"));
        }

        @Test
        @DisplayName("statusContrato alcança o histórico: contrato cancelado continua filtrável mesmo após a "
                + "propriedade voltar a DISPONIVEL (junção usa o agenciamento mais recente, não só contrato_ativo)")
        void statusContratoAlcancaHistoricoAposCancelamento() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID pessoaId = fixture.criarProprietario(tenantId);
            UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
            mapaFixture.geocodificarExistente(tenantId, propriedadeId, LAT_DENTRO, LON_DENTRO);
            UUID contratoId = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeId);
            mockMvc.perform(post("/api/v1/contratos/{id}/cancelamento", contratoId).with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("situacao", "DISPONIVEL")
                            .param("statusContrato", "CANCELADO")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(propriedadeId.toString()));
        }
    }

    @Nested
    @DisplayName("RN-07-03: filtro default de situação")
    class SituacaoDefault {

        @Test
        @DisplayName("sem filtro de situação, só propriedades AGENCIADA aparecem")
        void semFiltroSoTrazAgenciada() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            UUID agenciadaId = mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO,
                    SituacaoPropriedade.AGENCIADA);
            mapaFixture.criarGeocodificada(tenantId, proprietarioId, LAT_DENTRO, LON_DENTRO, SituacaoPropriedade.DISPONIVEL);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(1))
                    .andExpect(jsonPath("$.propriedades[0].id").value(agenciadaId.toString()));
        }
    }

    @Nested
    @DisplayName("RN-07-04: teto de 500 resultados")
    class TetoDeResultados {

        @Test
        @DisplayName("500 propriedades no bbox: todas retornam, sem sinalizar limite")
        void quinhentasPropriedadesNaoSinalizaLimite() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            mapaFixture.criarVariasGeocodificadas(tenantId, proprietarioId, 500, LAT_DENTRO, LON_DENTRO);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(500))
                    .andExpect(jsonPath("$.limitado").value(false));
        }

        @Test
        @DisplayName("501 propriedades no bbox: resposta é cortada em 500 e limitado=true")
        void quinhentasEUmaPropriedadesSinalizaLimite() throws Exception {
            UUID tenantId = fixture.criarTenant();
            UUID proprietarioId = fixture.criarProprietario(tenantId);
            mapaFixture.criarVariasGeocodificadas(tenantId, proprietarioId, 501, LAT_DENTRO, LON_DENTRO);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propriedades.length()").value(500))
                    .andExpect(jsonPath("$.limitado").value(true));
        }
    }

    @Nested
    @DisplayName("isolamento de tenant")
    class IsolamentoDeTenant {

        @Test
        @DisplayName("mapa nunca mistura propriedades de tenants diferentes, mesmo com coordenadas iguais")
        void naoMisturaPropriedadesDeOutroTenant() throws Exception {
            UUID tenantA = fixture.criarTenant();
            UUID tenantB = fixture.criarTenant();
            mapaFixture.criarGeocodificada(tenantA, fixture.criarProprietario(tenantA), LAT_DENTRO, LON_DENTRO);
            mapaFixture.criarGeocodificada(tenantB, fixture.criarProprietario(tenantB), LAT_DENTRO, LON_DENTRO);

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO)
                            .with(administradorDoTenant(tenantA)))
                    .andExpect(jsonPath("$.propriedades.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /mapa/propriedades — autenticação e autorização")
    class Autorizacao {

        @Test
        @DisplayName("sem token retorna 401")
        void semTokenRetorna401() throws Exception {
            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("sem papel USUARIO/ADMINISTRADOR retorna 403")
        void semPapelRetorna403() throws Exception {
            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", BBOX_SAO_PAULO).with(jwt()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("bbox ausente retorna 400 com código BOUNDING_BOX_INVALIDO")
        void bboxAusenteRetorna400() throws Exception {
            UUID tenantId = fixture.criarTenant();

            mockMvc.perform(get("/api/v1/mapa/propriedades").with(administradorDoTenant(tenantId)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("bbox malformado retorna 400 com código BOUNDING_BOX_INVALIDO")
        void bboxMalformadoRetorna400() throws Exception {
            UUID tenantId = fixture.criarTenant();

            mockMvc.perform(get("/api/v1/mapa/propriedades").param("bbox", "not-a-bbox")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("BOUNDING_BOX_INVALIDO"));
        }

        @Test
        @DisplayName("situacao inválida retorna 400 com código PARAMETRO_INVALIDO")
        void situacaoInvalidaRetorna400() throws Exception {
            UUID tenantId = fixture.criarTenant();

            mockMvc.perform(get("/api/v1/mapa/propriedades")
                            .param("bbox", BBOX_SAO_PAULO)
                            .param("situacao", "NAO_EXISTE")
                            .with(administradorDoTenant(tenantId)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        }
    }
}
