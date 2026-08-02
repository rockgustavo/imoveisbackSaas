package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PessoaTest {

    @Nested
    @DisplayName("RN-01-01: documento validado por dígito verificador")
    class Criacao {

        @Test
        @DisplayName("cria pessoa física com CPF válido")
        void criaComCpfValido() {
            String cpf = CpfTestFixture.novoCpfValido();
            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, cpf, "Fulano de Tal", null);

            assertThat(pessoa.getId()).isNotNull();
            assertThat(pessoa.getTipoDocumento()).isEqualTo(TipoDocumento.CPF);
            assertThat(pessoa.getDocumento()).isEqualTo(cpf);
            assertThat(pessoa.isAtivo()).isTrue();
        }

        @Test
        @DisplayName("cria pessoa jurídica com CNPJ válido")
        void criaComCnpjValido() {
            String cnpj = CnpjTestFixture.novoCnpjValido();
            Pessoa pessoa = new Pessoa(TipoDocumento.CNPJ, cnpj, "Empresa Exemplo Ltda", "contato@exemplo.com");

            assertThat(pessoa.getTipoDocumento()).isEqualTo(TipoDocumento.CNPJ);
            assertThat(pessoa.getDocumento()).isEqualTo(cnpj);
            assertThat(pessoa.getEmail()).isEqualTo("contato@exemplo.com");
        }

        @Test
        @DisplayName("normaliza documento formatado removendo máscara")
        void normalizaDocumentoFormatado() {
            String cpf = CpfTestFixture.novoCpfValido();
            String comMascara = "%s.%s.%s-%s".formatted(
                    cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9), cpf.substring(9, 11));

            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, comMascara, "Fulano de Tal", null);

            assertThat(pessoa.getDocumento()).isEqualTo(cpf);
        }

        @Test
        @DisplayName("rejeita CPF com dígito verificador inválido")
        void rejeitaCpfInvalido() {
            assertThatThrownBy(() -> new Pessoa(TipoDocumento.CPF, "11111111111", "Fulano de Tal", null))
                    .isInstanceOf(DocumentoInvalidoException.class);
        }

        @Test
        @DisplayName("rejeita nome vazio")
        void rejeitaNomeVazio() {
            assertThatThrownBy(() -> new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), " ", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("RN-01-05: inativação lógica")
    class Inativacao {

        @Test
        @DisplayName("inativar marca ativo como falso")
        void inativarMarcaAtivoComoFalso() {
            Pessoa pessoa = new Pessoa(TipoDocumento.CPF, CpfTestFixture.novoCpfValido(), "Fulano de Tal", null);

            pessoa.inativar();

            assertThat(pessoa.isAtivo()).isFalse();
        }
    }
}
