package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.CredencialProvisionamentoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Pessoa;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PessoaPapelProprioImutavelException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaPapelRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaQueryRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaRepository;
import br.com.rockgustavo.imobiliaria.shared.security.AcessoAtivoCache;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock
    PessoaRepository pessoaRepository;

    @Mock
    PessoaPapelRepository papelRepository;

    @Mock
    PessoaQueryRepository queryRepository;

    @Mock
    KeycloakAdminPort keycloakAdminPort;

    @Mock
    AcessoAtivoCache acessoAtivoCache;

    @InjectMocks
    PessoaService service;

    @Nested
    @DisplayName("RN-01-10: provisionamento de credencial")
    class ProvisionamentoDeCredencial {

        @Test
        @DisplayName("falha do adaptador Keycloak não deixa papel órfão sem subject_idp")
        void falhaDoAdaptadorNaoDeixaPapelOrfao() {
            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Fulano de Tal", null);
            UUID pessoaId = pessoa.getId();
            when(pessoaRepository.buscarPorId(pessoaId)).thenReturn(Optional.of(pessoa));
            when(keycloakAdminPort.provisionar(eq("fulano@exemplo.com"), eq("Fulano de Tal"), any()))
                    .thenThrow(new CredencialProvisionamentoException("fulano@exemplo.com", null));

            AtribuirPapelComando comando = new AtribuirPapelComando(pessoaId, Papel.USUARIO, "fulano@exemplo.com");

            assertThatThrownBy(() -> service.atribuirPapel(comando))
                    .isInstanceOf(CredencialProvisionamentoException.class);

            assertThat(pessoa.getSubjectIdp()).isNull();
            verify(papelRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("RN-02-03: usuário não altera os próprios papéis")
    class AutoAlteracaoDePapel {

        @AfterEach
        void limparContextoDeSeguranca() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("atribuir papel a si mesmo é rejeitado")
        void atribuirPapelAoProprioUsuarioEhRejeitado() {
            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Fulano de Tal", "fulano@exemplo.com");
            autenticarComo("subject-fulano");
            when(pessoaRepository.findBySubjectIdp("subject-fulano")).thenReturn(Optional.of(pessoa));

            AtribuirPapelComando comando = new AtribuirPapelComando(pessoa.getId(), Papel.USUARIO, "fulano@exemplo.com");

            assertThatThrownBy(() -> service.atribuirPapel(comando))
                    .isInstanceOf(PessoaPapelProprioImutavelException.class);
            verify(papelRepository, never()).save(any());
        }

        @Test
        @DisplayName("remover papel do próprio usuário é rejeitado")
        void removerPapelDoProprioUsuarioEhRejeitado() {
            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Fulano de Tal", "fulano@exemplo.com");
            autenticarComo("subject-fulano");
            when(pessoaRepository.findBySubjectIdp("subject-fulano")).thenReturn(Optional.of(pessoa));

            assertThatThrownBy(() -> service.removerPapel(pessoa.getId(), Papel.ADMINISTRADOR))
                    .isInstanceOf(PessoaPapelProprioImutavelException.class);
            verify(papelRepository, never()).delete(any());
        }

        @Test
        @DisplayName("atribuir papel a outra pessoa é permitido")
        void atribuirPapelAOutraPessoaEhPermitido() {
            Pessoa autor = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Autora", "autora@exemplo.com");
            Pessoa alvo = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Alvo", null);
            autenticarComo("subject-autora");
            when(pessoaRepository.findBySubjectIdp("subject-autora")).thenReturn(Optional.of(autor));
            when(pessoaRepository.buscarPorId(alvo.getId())).thenReturn(Optional.of(alvo));

            AtribuirPapelComando comando = new AtribuirPapelComando(alvo.getId(), Papel.PROPRIETARIO, null);

            service.atribuirPapel(comando);

            verify(papelRepository).save(any());
        }

        private void autenticarComo(String subjectIdp) {
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(subjectIdp)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        }
    }
}
