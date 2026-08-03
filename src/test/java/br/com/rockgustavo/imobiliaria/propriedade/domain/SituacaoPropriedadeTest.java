package br.com.rockgustavo.imobiliaria.propriedade.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-03-06: matriz de transições de situação")
class SituacaoPropriedadeTest {

    @ParameterizedTest(name = "{0} → {1} é {2}")
    @CsvSource({
            "DISPONIVEL, AGENCIADA, true",
            "DISPONIVEL, RESERVADA, false",
            "DISPONIVEL, VENDIDA, false",
            "DISPONIVEL, RETIRADA, true",
            "DISPONIVEL, DISPONIVEL, false",
            "AGENCIADA, DISPONIVEL, true",
            "AGENCIADA, RESERVADA, true",
            "AGENCIADA, VENDIDA, false",
            "AGENCIADA, RETIRADA, false",
            "AGENCIADA, AGENCIADA, false",
            "RESERVADA, DISPONIVEL, false",
            "RESERVADA, AGENCIADA, true",
            "RESERVADA, VENDIDA, true",
            "RESERVADA, RETIRADA, false",
            "RESERVADA, RESERVADA, false",
            "VENDIDA, DISPONIVEL, false",
            "VENDIDA, AGENCIADA, false",
            "VENDIDA, RESERVADA, false",
            "VENDIDA, RETIRADA, false",
            "VENDIDA, VENDIDA, false",
            "RETIRADA, DISPONIVEL, false",
            "RETIRADA, AGENCIADA, false",
            "RETIRADA, RESERVADA, false",
            "RETIRADA, VENDIDA, false",
            "RETIRADA, RETIRADA, false"
    })
    void matrizDeTransicoes(SituacaoPropriedade origem, SituacaoPropriedade destino, boolean esperado) {
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(esperado);
    }
}
