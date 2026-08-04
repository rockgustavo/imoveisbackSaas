package br.com.rockgustavo.imobiliaria.orcamento.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-05-02: matriz de transições de status")
class StatusOrcamentoTest {

    @ParameterizedTest(name = "{0} → {1} é {2}")
    @CsvSource({
            "RASCUNHO, ENVIADO, true",
            "RASCUNHO, ACEITO, false",
            "RASCUNHO, RECUSADO, false",
            "RASCUNHO, EXPIRADO, false",
            "RASCUNHO, RASCUNHO, false",
            "ENVIADO, RASCUNHO, false",
            "ENVIADO, ACEITO, true",
            "ENVIADO, RECUSADO, true",
            "ENVIADO, EXPIRADO, true",
            "ENVIADO, ENVIADO, false",
            "ACEITO, RASCUNHO, false",
            "ACEITO, ENVIADO, false",
            "ACEITO, RECUSADO, false",
            "ACEITO, EXPIRADO, false",
            "ACEITO, ACEITO, false",
            "RECUSADO, RASCUNHO, false",
            "RECUSADO, ENVIADO, false",
            "RECUSADO, ACEITO, false",
            "RECUSADO, EXPIRADO, false",
            "RECUSADO, RECUSADO, false",
            "EXPIRADO, RASCUNHO, false",
            "EXPIRADO, ENVIADO, false",
            "EXPIRADO, ACEITO, false",
            "EXPIRADO, RECUSADO, false",
            "EXPIRADO, EXPIRADO, false"
    })
    void matrizDeTransicoes(StatusOrcamento origem, StatusOrcamento destino, boolean esperado) {
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(esperado);
    }
}
