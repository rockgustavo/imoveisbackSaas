package br.com.rockgustavo.imobiliaria.shared.auditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RN-09-02: histórico de transição é imutável")
class HistoricoTransicaoRepositoryTest {

    @Test
    @DisplayName("repositório não expõe nenhum método de update ou delete")
    void naoExpoeMetodoDeUpdateOuDelete() {
        boolean existeMetodoDeEscritaDestrutiva = Arrays.stream(HistoricoTransicaoRepository.class.getMethods())
                .map(Method::getName)
                .map(String::toLowerCase)
                .anyMatch(nome -> nome.startsWith("delete") || nome.startsWith("update") || nome.startsWith("remove"));

        assertThat(existeMetodoDeEscritaDestrutiva).isFalse();
    }
}
