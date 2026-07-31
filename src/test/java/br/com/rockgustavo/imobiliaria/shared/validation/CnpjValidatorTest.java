package br.com.rockgustavo.imobiliaria.shared.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    @Test
    void aceitaCnpjComDigitoVerificadorCorreto() {
        assertThat(CnpjValidator.valido(CnpjTestFixture.novoCnpjValido())).isTrue();
    }

    @Test
    void rejeitaCnpjComTodosDigitosIguais() {
        assertThat(CnpjValidator.valido("11111111111111")).isFalse();
    }

    @Test
    void rejeitaCnpjComTamanhoErrado() {
        assertThat(CnpjValidator.valido("123")).isFalse();
    }

    @Test
    void rejeitaCnpjComDigitoVerificadorErrado() {
        String cnpj = CnpjTestFixture.novoCnpjValido();
        char ultimoDigito = cnpj.charAt(13);
        char digitoTrocado = ultimoDigito == '0' ? '1' : '0';
        String cnpjAdulterado = cnpj.substring(0, 13) + digitoTrocado;

        assertThat(CnpjValidator.valido(cnpjAdulterado)).isFalse();
    }

    @Test
    void rejeitaCnpjNulo() {
        assertThat(CnpjValidator.valido(null)).isFalse();
    }
}
