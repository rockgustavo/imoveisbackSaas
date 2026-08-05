package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ContadorDeQueriesConfig.class)
class ContratoListagemContagemDeQueriesIT extends AbstractIntegrationTest {

    private static final int LIMITE_DE_QUERIES = 2;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ContadorDeQueriesDataSource dataSourceContandoQueries;

    @Test
    @DisplayName("GET /contratos: uma query de contagem e uma de conteúdo com JOIN, sem N+1 por item (CLAUDE.md §4)")
    void listagemNaoTemNMaisUm() throws Exception {
        UUID tenantId = criarTenant();
        UUID pessoaId = criarProprietario(tenantId);
        UUID propriedadeA = criarPropriedadeEObterId(tenantId, pessoaId);
        UUID propriedadeB = criarPropriedadeEObterId(tenantId, pessoaId);
        criarContratoEObterId(tenantId, criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeA));
        criarContratoEObterId(tenantId, criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeB));

        dataSourceContandoQueries.zerar();
        mockMvc.perform(get("/api/v1/contratos").with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());

        assertThat(dataSourceContandoQueries.contagemAtual()).isLessThanOrEqualTo(LIMITE_DE_QUERIES);
    }

    private UUID criarOrcamentoAceitoEObterId(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        String corpo = """
                {"pessoaId":"%s","itens":[{"propriedadeId":"%s","comissaoPercentual":5.00,"valorPedido":450000.00}]}
                """.formatted(pessoaId, propriedadeId);
        String location = mockMvc.perform(post("/api/v1/orcamentos")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        UUID orcamentoId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
        mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        return orcamentoId;
    }

    private UUID criarContratoEObterId(UUID tenantId, UUID orcamentoId) throws Exception {
        String corpo = """
                {"orcamentoId":"%s","vigenciaInicio":"%s","vigenciaFim":"%s","regrasContratuais":"regras de teste"}
                """.formatted(orcamentoId, LocalDate.now(), LocalDate.now().plusYears(1));
        String location = mockMvc.perform(post("/api/v1/contratos")
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
