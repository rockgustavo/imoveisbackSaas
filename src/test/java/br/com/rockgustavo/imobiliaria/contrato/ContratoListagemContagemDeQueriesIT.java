package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
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
class ContratoListagemContagemDeQueriesIT extends AbstractIntegrationTest {

    private static final int LIMITE_DE_QUERIES = 2;

    @Autowired
    ContadorDeQueriesDataSource dataSourceContandoQueries;

    @Test
    @DisplayName("GET /contratos: uma query de contagem e uma de conteúdo com JOIN, sem N+1 por item (CLAUDE.md §4)")
    void listagemNaoTemNMaisUm() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID pessoaId = fixture.criarProprietario(tenantId);
        UUID propriedadeA = fixture.criarPropriedade(tenantId, pessoaId);
        UUID propriedadeB = fixture.criarPropriedade(tenantId, pessoaId);
        fixture.criarContrato(tenantId, fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeA));
        fixture.criarContrato(tenantId, fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeB));

        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/contratos").with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());

        assertThat(dataSourceContandoQueries.contagemAtual()).isLessThanOrEqualTo(LIMITE_DE_QUERIES);
    }
}
