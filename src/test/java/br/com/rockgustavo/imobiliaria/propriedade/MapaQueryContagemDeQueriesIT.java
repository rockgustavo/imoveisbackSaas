package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesConfig;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesDataSource;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ContadorDeQueriesConfig.class)
class MapaQueryContagemDeQueriesIT extends AbstractIntegrationTest {

    private static final int LIMITE_DE_QUERIES = 1;

    @Autowired
    PropriedadeRepository propriedadeRepository;

    @Autowired
    ContadorDeQueriesDataSource dataSourceContandoQueries;

    MapaTestFixture mapaFixture;

    @BeforeEach
    void configurarFixture() {
        mapaFixture = new MapaTestFixture(propriedadeRepository);
    }

    @Test
    @DisplayName("GET /mapa/propriedades: uma única query com LEFT JOIN LATERAL, sem N+1 por item (CLAUDE.md §4)")
    void buscaNoMapaNaoTemNMaisUm() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID proprietarioId = fixture.criarProprietario(tenantId);
        mapaFixture.criarVariasGeocodificadas(tenantId, proprietarioId, 5, new BigDecimal("-23.55"), new BigDecimal("-46.65"));

        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/mapa/propriedades")
                        .param("bbox", "-23.60,-46.70,-23.50,-46.60")
                        .with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());

        assertThat(dataSourceContandoQueries.contagemAtual()).isLessThanOrEqualTo(LIMITE_DE_QUERIES);
    }
}
