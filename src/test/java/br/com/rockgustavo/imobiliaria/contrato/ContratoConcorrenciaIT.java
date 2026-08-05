package br.com.rockgustavo.imobiliaria.contrato;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoService;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoVigenciaSobrepostaException;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;

class ContratoConcorrenciaIT extends AbstractIntegrationTest {

    @Autowired
    ContratoService contratoService;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("RN-06-05: sob ativação concorrente da mesma propriedade com vigências sobrepostas, o EXCLUDE do banco "
            + "decide — exatamente um contrato ativa, o outro é rejeitado")
    void duasAtivacoesConcorrentesApenasUmaVence() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID pessoaId = fixture.criarProprietario(tenantId);
        UUID propriedadeId = fixture.criarPropriedade(tenantId, pessoaId);
        UUID orcamento1 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
        UUID orcamento2 = fixture.criarOrcamentoAceito(tenantId, pessoaId, propriedadeId);
        UUID contrato1 = fixture.criarContrato(tenantId, orcamento1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        UUID contrato2 = fixture.criarContrato(tenantId, orcamento2, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

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
}
