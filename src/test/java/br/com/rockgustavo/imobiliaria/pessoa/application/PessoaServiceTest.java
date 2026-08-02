package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.CredencialProvisionamentoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Pessoa;
import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaPapelRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaQueryRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaRepository;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
            when(keycloakAdminPort.provisionar("fulano@exemplo.com", "Fulano de Tal"))
                    .thenThrow(new CredencialProvisionamentoException("fulano@exemplo.com", null));

            AtribuirPapelComando comando = new AtribuirPapelComando(pessoaId, Papel.USUARIO, "fulano@exemplo.com");

            assertThatThrownBy(() -> service.atribuirPapel(comando))
                    .isInstanceOf(CredencialProvisionamentoException.class);

            assertThat(pessoa.getSubjectIdp()).isNull();
            verify(papelRepository, never()).save(any());
        }
    }
}
