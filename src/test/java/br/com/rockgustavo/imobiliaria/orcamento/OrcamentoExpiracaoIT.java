package br.com.rockgustavo.imobiliaria.orcamento;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.orcamento.application.ExpiracaoOrcamentoJob;
import br.com.rockgustavo.imobiliaria.orcamento.domain.Orcamento;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrcamentoExpiracaoIT extends AbstractIntegrationTest {

    @Autowired
    OrcamentoRepository orcamentoRepository;

    @Autowired
    ExpiracaoOrcamentoJob expiracaoOrcamentoJob;

    @Test
    @DisplayName("RN-05-03: orçamento ENVIADO com validade vencida expira ao rodar a rotina, e o aceite passa a ser rejeitado")
    void orcamentoVencidoExpiraAoRodarRotina() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID pessoaId = fixture.criarProprietario(tenantId);
        UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
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
            Orcamento orcamento = new Orcamento(pessoaId, LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1), List.of(
                    new Orcamento.ItemProposto(propriedadeId, new BigDecimal("5.00"), new BigDecimal("450000.00"))));
            orcamento.enviar();
            orcamentoRepository.save(orcamento);
            return orcamento.getId();
        } finally {
            TenantContext.limpar();
        }
    }
}
