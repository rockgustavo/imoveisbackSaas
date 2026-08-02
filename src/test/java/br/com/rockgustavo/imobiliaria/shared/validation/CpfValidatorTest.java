package br.com.rockgustavo.imobiliaria.shared.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @Test
    void aceitaCpfComDigitoVerificadorCorreto() {
        assertThat(CpfValidator.valido(CpfTestFixture.novoCpfValido())).isTrue();
    }

    @Test
    void rejeitaCpfComTodosDigitosIguais() {
        assertThat(CpfValidator.valido("11111111111")).isFalse();
    }

    @Test
    void rejeitaCpfComTamanhoErrado() {
        assertThat(CpfValidator.valido("123")).isFalse();
    }

    @Test
    void rejeitaCpfComDigitoVerificadorErrado() {
        String cpf = CpfTestFixture.novoCpfValido();
        char ultimoDigito = cpf.charAt(10);
        char digitoTrocado = ultimoDigito == '0' ? '1' : '0';
        String cpfAdulterado = cpf.substring(0, 10) + digitoTrocado;

        assertThat(CpfValidator.valido(cpfAdulterado)).isFalse();
    }

    @Test
    void rejeitaCpfNulo() {
        assertThat(CpfValidator.valido(null)).isFalse();
    }
}
