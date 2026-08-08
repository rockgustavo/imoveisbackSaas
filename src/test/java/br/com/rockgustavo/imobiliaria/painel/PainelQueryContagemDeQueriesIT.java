package br.com.rockgustavo.imobiliaria.painel;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesConfig;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ContadorDeQueriesConfig.class)
class PainelQueryContagemDeQueriesIT extends AbstractIntegrationTest {

    private static final int LIMITE_DE_QUERIES = 4;

    @Autowired
    ContadorDeQueriesDataSource dataSourceContandoQueries;

    @Test
    @DisplayName("GET /painel/indicadores: uma query por grupo de indicador + parâmetros do tenant, sem N+1 (CLAUDE.md §4)")
    void buscaDeIndicadoresNaoTemNMaisUm() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID proprietario = fixture.criarProprietario(tenantId);
        fixture.criarContratoAtivo(tenantId, proprietario, fixture.criarPropriedade(tenantId, proprietario));
        aguardarEventosDeAuditoriaAssentarem();

        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/painel/indicadores").with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());

        assertThat(dataSourceContandoQueries.contagemAtual()).isLessThanOrEqualTo(LIMITE_DE_QUERIES);
    }
}
