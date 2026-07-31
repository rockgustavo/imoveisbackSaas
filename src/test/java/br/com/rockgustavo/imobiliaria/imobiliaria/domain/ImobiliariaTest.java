package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImobiliariaTest {

    @Nested
    @DisplayName("RN-00-06: imobiliária como entidade")
    class Criacao {

        @Test
        @DisplayName("cria com status ATIVA e id gerado")
        void criaComSucesso() {
            String cnpj = CnpjTestFixture.novoCnpjValido();
            Imobiliaria imobiliaria = new Imobiliaria("Corretora Exemplo Ltda", cnpj, "corretora-exemplo");

            assertThat(imobiliaria.getId()).isNotNull();
            assertThat(imobiliaria.getStatus()).isEqualTo(StatusImobiliaria.ATIVA);
            assertThat(imobiliaria.getCnpj()).isEqualTo(cnpj);
        }

        @Test
        @DisplayName("normaliza CNPJ formatado removendo máscara")
        void normalizaCnpjFormatado() {
            String cnpj = CnpjTestFixture.novoCnpjValido();
            String comMascara = "%s.%s.%s/%s-%s".formatted(
                    cnpj.substring(0, 2), cnpj.substring(2, 5), cnpj.substring(5, 8),
                    cnpj.substring(8, 12), cnpj.substring(12, 14));

            Imobiliaria imobiliaria = new Imobiliaria("Corretora Exemplo Ltda", comMascara, "corretora-exemplo");

            assertThat(imobiliaria.getCnpj()).isEqualTo(cnpj);
        }

        @Test
        @DisplayName("rejeita CNPJ com dígito verificador inválido")
        void rejeitaCnpjInvalido() {
            assertThatThrownBy(() -> new Imobiliaria("Corretora Exemplo Ltda", "11111111111111", "corretora-exemplo"))
                    .isInstanceOf(CnpjInvalidoException.class);
        }

        @Test
        @DisplayName("rejeita slug com maiúscula ou espaço")
        void rejeitaSlugInvalido() {
            assertThatThrownBy(() -> new Imobiliaria("Corretora Exemplo Ltda", CnpjTestFixture.novoCnpjValido(), "Corretora Exemplo"))
                    .isInstanceOf(SlugInvalidoException.class);
        }

        @Test
        @DisplayName("rejeita razão social vazia")
        void rejeitaRazaoSocialVazia() {
            assertThatThrownBy(() -> new Imobiliaria(" ", CnpjTestFixture.novoCnpjValido(), "corretora-exemplo"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
