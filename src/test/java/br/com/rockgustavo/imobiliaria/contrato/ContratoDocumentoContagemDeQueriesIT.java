package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesConfig;
import br.com.rockgustavo.imobiliaria.ContadorDeQueriesDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ContadorDeQueriesConfig.class)
class ContratoDocumentoContagemDeQueriesIT extends AbstractIntegrationTest {

    @Autowired
    ContadorDeQueriesDataSource dataSourceContandoQueries;

    @Test
    @DisplayName("GET /documento: número de queries não cresce com a quantidade de imóveis agenciados (sem N+1, CLAUDE.md §4)")
    void geracaoDeDocumentoNaoTemNMaisUm() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID pessoaId = fixture.criarProprietario(tenantId);

        UUID propriedadeUnica = fixture.criarPropriedade(tenantId, pessoaId);
        UUID contratoComUmImovel = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeUnica);
        aguardarEventosDeAuditoriaAssentarem();
        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoComUmImovel).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        int queriesComUmImovel = dataSourceContandoQueries.contagemAtual();

        UUID propriedadeA = fixture.criarPropriedade(tenantId, pessoaId);
        UUID propriedadeB = fixture.criarPropriedade(tenantId, pessoaId);
        UUID propriedadeC = fixture.criarPropriedade(tenantId, pessoaId);
        UUID contratoComTresImoveis = fixture.criarContratoAtivo(tenantId, pessoaId, propriedadeA);
        registrarAditivoInclusao(tenantId, contratoComTresImoveis, propriedadeB);
        registrarAditivoInclusao(tenantId, contratoComTresImoveis, propriedadeC);
        aguardarEventosDeAuditoriaAssentarem();
        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/contratos/{id}/documento", contratoComTresImoveis).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        int queriesComTresImoveis = dataSourceContandoQueries.contagemAtual();

        assertThat(queriesComTresImoveis).isEqualTo(queriesComUmImovel);
    }

    private void registrarAditivoInclusao(UUID tenantId, UUID contratoId, UUID propriedadeId) throws Exception {
        mockMvc.perform(post("/api/v1/contratos/{id}/aditivos", contratoId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"INCLUSAO","propriedadeId":"%s","justificativa":"item adicional",
                                 "comissaoPercentual":5.00,"valorPedido":100000.00}
                                """.formatted(propriedadeId)))
                .andExpect(status().isOk());
    }
}
