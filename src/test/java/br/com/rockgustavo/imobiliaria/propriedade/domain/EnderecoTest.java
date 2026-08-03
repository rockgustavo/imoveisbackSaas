package br.com.rockgustavo.imobiliaria.propriedade.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RN-03-02/03: endereço como snapshot")
class EnderecoTest {

    @Test
    @DisplayName("normaliza CEP removendo máscara")
    void normalizaCepFormatado() {
        Endereco endereco = new Endereco("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", true);

        assertThat(endereco.getCep()).isEqualTo("01310100");
    }

    @Test
    @DisplayName("rejeita CEP com menos de 8 dígitos")
    void rejeitaCepCurto() {
        assertThatThrownBy(() -> new Endereco("123", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("normaliza UF para maiúsculas")
    void normalizaUfParaMaiusculas() {
        Endereco endereco = new Endereco("01310100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "sp", true);

        assertThat(endereco.getUf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("rejeita UF fora do padrão de 2 letras")
    void rejeitaUfInvalida() {
        assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SPX", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RN-03-03: aceita número não numérico, como S/N")
    void aceitaNumeroNaoNumerico() {
        Endereco endereco = new Endereco("01310100", "Rodovia BR-101", "S/N", null, "Zona Rural", "Cidade", "SP", true);

        assertThat(endereco.getNumero()).isEqualTo("S/N");
    }

    @Test
    @DisplayName("rejeita bairro vazio")
    void rejeitaBairroVazio() {
        assertThatThrownBy(() -> new Endereco("01310100", "Av. Paulista", "1000", null, " ", "São Paulo", "SP", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RN-03-04: marca não validado quando CEP não foi encontrado")
    void marcaNaoValidadoQuandoCepNaoEncontrado() {
        Endereco endereco = new Endereco("99999999", "Rua Preenchida Manualmente", "10", null, "Bairro", "Cidade",
                "SP", false);

        assertThat(endereco.isValidado()).isFalse();
    }
}
