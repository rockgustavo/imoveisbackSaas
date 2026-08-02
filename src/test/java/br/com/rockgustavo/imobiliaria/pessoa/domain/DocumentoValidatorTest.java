package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoValidatorTest {

    @Test
    void validaCpfComDigitoVerificadorCorreto() {
        String cpf = CpfTestFixture.novoCpfValido();
        assertThat(DocumentoValidator.validar(TipoDocumento.CPF, cpf)).isEqualTo(cpf);
    }

    @Test
    void validaCnpjComDigitoVerificadorCorreto() {
        String cnpj = CnpjTestFixture.novoCnpjValido();
        assertThat(DocumentoValidator.validar(TipoDocumento.CNPJ, cnpj)).isEqualTo(cnpj);
    }

    @Test
    void rejeitaCpfComDigitoVerificadorInvalido() {
        assertThatThrownBy(() -> DocumentoValidator.validar(TipoDocumento.CPF, "11111111111"))
                .isInstanceOf(DocumentoInvalidoException.class);
    }

    @Test
    void rejeitaCnpjComDigitoVerificadorInvalido() {
        assertThatThrownBy(() -> DocumentoValidator.validar(TipoDocumento.CNPJ, "11111111111111"))
                .isInstanceOf(DocumentoInvalidoException.class);
    }
}
