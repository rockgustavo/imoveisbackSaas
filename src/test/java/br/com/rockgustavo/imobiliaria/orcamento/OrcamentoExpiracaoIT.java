package br.com.rockgustavo.imobiliaria.orcamento;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.orcamento.application.ExpiracaoOrcamentoJob;
import br.com.rockgustavo.imobiliaria.orcamento.domain.Orcamento;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrcamentoExpiracaoIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrcamentoRepository orcamentoRepository;

    @Autowired
    ExpiracaoOrcamentoJob expiracaoOrcamentoJob;

    @Test
    @DisplayName("RN-05-03: orçamento ENVIADO com validade vencida expira ao rodar a rotina, e o aceite passa a ser rejeitado")
    void orcamentoVencidoExpiraAoRodarRotina() throws Exception {
        UUID tenantId = criarTenant();
        UUID pessoaId = criarProprietario(tenantId);
        UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
        UUID orcamentoId = semearOrcamentoEnviadoVencidoDiretoPeloRepositorio(tenantId, pessoaId, propriedadeId);

        expiracaoOrcamentoJob.executar();

        mockMvc.perform(get("/api/v1/orcamentos/{id}", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRADO"));
        mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("ORCAMENTO_EXPIRADO_OU_RECUSADO"));
    }

    private UUID semearOrcamentoEnviadoVencidoDiretoPeloRepositorio(UUID tenantId, UUID pessoaId, UUID propriedadeId) {
        TenantContext.definir(tenantId);
        try {
            Orcamento orcamento = new Orcamento(pessoaId, LocalDate.now().minusDays(1), List.of(
                    new Orcamento.ItemProposto(propriedadeId, new BigDecimal("5.00"), new BigDecimal("450000.00"))));
            orcamento.enviar();
            orcamentoRepository.save(orcamento);
            return orcamento.getId();
        } finally {
            TenantContext.limpar();
        }
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
