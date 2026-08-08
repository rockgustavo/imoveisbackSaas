package br.com.rockgustavo.imobiliaria.shared.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-00-05: autor de criação/alteração")
class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class ComRequisicaoAutenticada {

        @Test
        @DisplayName("resolve o autor a partir da claim pessoa_id do token")
        void resolveAutorDaClaim() {
            UUID pessoaId = UUID.randomUUID();
            autenticarComo(pessoaId);

            Optional<UUID> autor = auditorAware.getCurrentAuditor();

            assertThat(autor).contains(pessoaId);
        }

        private void autenticarComo(UUID pessoaId) {
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject("subject-teste")
                    .claim(SecurityAuditorAware.CLAIM_PESSOA_ID, pessoaId.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        }
    }

    @Nested
    class SemRequisicaoAutenticada {

        @Test
        @DisplayName("rotina automática (sem SecurityContext) grava o autor sistêmico reservado")
        void semAutenticacaoUsaAutorSistemico() {
            SecurityContextHolder.clearContext();

            Optional<UUID> autor = auditorAware.getCurrentAuditor();

            assertThat(autor).contains(SistemaAutor.ID);
        }

        @Test
        @DisplayName("principal autenticado que não é um token JWT também usa o autor sistêmico")
        void principalNaoJwtUsaAutorSistemico() {
            SecurityContextHolder.getContext()
                    .setAuthentication(new TestingAuthenticationToken("usuario-tecnico", null));

            Optional<UUID> autor = auditorAware.getCurrentAuditor();

            assertThat(autor).contains(SistemaAutor.ID);
        }
    }
}
