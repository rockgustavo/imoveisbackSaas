package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoService;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoVigenciaSobrepostaException;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CyclicBarrier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContratoConcorrenciaIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ContratoService contratoService;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("RN-06-05: sob ativação concorrente da mesma propriedade com vigências sobrepostas, o EXCLUDE do banco "
            + "decide — exatamente um contrato ativa, o outro é rejeitado")
    void duasAtivacoesConcorrentesApenasUmaVence() throws Exception {
        UUID tenantId = criarTenant();
        UUID pessoaId = criarProprietario(tenantId);
        UUID propriedadeId = criarPropriedadeEObterId(tenantId, pessoaId);
        UUID orcamento1 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
        UUID orcamento2 = criarOrcamentoAceitoEObterId(tenantId, pessoaId, propriedadeId);
        UUID contrato1 = criarContratoEObterId(tenantId, orcamento1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        UUID contrato2 = criarContratoEObterId(tenantId, orcamento2, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barreira = new CyclicBarrier(2);
        int sucessos = 0;
        int falhasPorSobreposicao = 0;
        try {
            List<Future<UUID>> resultados = executor.invokeAll(List.of(
                    ativarComContextoProprio(tenantId, contrato1, barreira),
                    ativarComContextoProprio(tenantId, contrato2, barreira)));

            for (Future<UUID> resultado : resultados) {
                try {
                    resultado.get();
                    sucessos++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ContratoVigenciaSobrepostaException.class);
                    falhasPorSobreposicao++;
                }
            }
        } finally {
            executor.shutdown();
        }

        assertThat(sucessos).isEqualTo(1);
        assertThat(falhasPorSobreposicao).isEqualTo(1);

        Long agenciamentosAtivos = jdbcClient.sql("""
                SELECT count(*) FROM agenciamento
                 WHERE tenant_id = :tenantId AND propriedade_id = :propriedadeId AND contrato_ativo = true
                """)
                .param("tenantId", tenantId)
                .param("propriedadeId", propriedadeId)
                .query(Long.class)
                .single();
        assertThat(agenciamentosAtivos).isEqualTo(1L);
    }

    private Callable<UUID> ativarComContextoProprio(UUID tenantId, UUID contratoId, CyclicBarrier barreira) {
        return () -> {
            SecurityContextHolder.setContext(contextoAdministrador(tenantId));
            TenantContext.definir(tenantId);
            try {
                barreira.await();
                contratoService.ativar(contratoId);
                return contratoId;
            } finally {
                TenantContext.limpar();
                SecurityContextHolder.clearContext();
            }
        };
    }

    private SecurityContext contextoAdministrador(UUID tenantId) {
        Jwt jwt = Jwt.withTokenValue("teste-concorrencia")
                .header("alg", "none")
                .claim("tenant_id", tenantId.toString())
                .subject("teste-concorrencia")
                .build();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    private UUID criarContratoEObterId(UUID tenantId, UUID orcamentoId, LocalDate inicio, LocalDate fim) throws Exception {
        String corpo = """
                {"orcamentoId":"%s","vigenciaInicio":"%s","vigenciaFim":"%s","regrasContratuais":"regras de teste"}
                """.formatted(orcamentoId, inicio, fim);
        String location = mockMvc.perform(post("/api/v1/contratos")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
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
