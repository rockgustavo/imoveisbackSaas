package br.com.rockgustavo.imobiliaria.contrato.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-06-04: matriz de transições de status")
class StatusContratoTest {

    @ParameterizedTest(name = "{0} → {1} é {2}")
    @CsvSource({
            "RASCUNHO, RASCUNHO, false",
            "RASCUNHO, ATIVO, true",
            "RASCUNHO, ENCERRADO, false",
            "RASCUNHO, CANCELADO, true",
            "RASCUNHO, EXPIRADO, false",
            "ATIVO, RASCUNHO, false",
            "ATIVO, ATIVO, false",
            "ATIVO, ENCERRADO, true",
            "ATIVO, CANCELADO, true",
            "ATIVO, EXPIRADO, true",
            "ENCERRADO, RASCUNHO, false",
            "ENCERRADO, ATIVO, false",
            "ENCERRADO, ENCERRADO, false",
            "ENCERRADO, CANCELADO, false",
            "ENCERRADO, EXPIRADO, false",
            "CANCELADO, RASCUNHO, false",
            "CANCELADO, ATIVO, false",
            "CANCELADO, ENCERRADO, false",
            "CANCELADO, CANCELADO, false",
            "CANCELADO, EXPIRADO, false",
            "EXPIRADO, RASCUNHO, false",
            "EXPIRADO, ATIVO, false",
            "EXPIRADO, ENCERRADO, false",
            "EXPIRADO, CANCELADO, false",
            "EXPIRADO, EXPIRADO, false"
    })
    void matrizDeTransicoes(StatusContrato origem, StatusContrato destino, boolean esperado) {
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(esperado);
    }
}
