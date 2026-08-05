package br.com.rockgustavo.imobiliaria.propriedade.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RN-07-04: bounding box do mapa")
class BoundingBoxTest {

    @Nested
    @DisplayName("parse de string válida")
    class ParseValido {

        @Test
        @DisplayName("aceita minLat,minLon,maxLat,maxLon")
        void aceitaQuatroValores() {
            BoundingBox bbox = BoundingBox.parse("-23.60,-46.70,-23.50,-46.60");

            assertThat(bbox.minLat()).isEqualByComparingTo("-23.60");
            assertThat(bbox.minLon()).isEqualByComparingTo("-46.70");
            assertThat(bbox.maxLat()).isEqualByComparingTo("-23.50");
            assertThat(bbox.maxLon()).isEqualByComparingTo("-46.60");
        }

        @Test
        @DisplayName("ignora espaços em volta de cada valor")
        void ignoraEspacos() {
            BoundingBox bbox = BoundingBox.parse(" -23.60 , -46.70 , -23.50 , -46.60 ");

            assertThat(bbox.minLat()).isEqualByComparingTo("-23.60");
        }

        @Test
        @DisplayName("aceita os extremos do intervalo global quando min < max")
        void aceitaExtremosGlobais() {
            BoundingBox bbox = BoundingBox.parse("-90,-180,90,180");

            assertThat(bbox.minLat()).isEqualByComparingTo("-90");
            assertThat(bbox.maxLon()).isEqualByComparingTo("180");
        }
    }

    @Nested
    @DisplayName("parse de string inválida — BOUNDING_BOX_INVALIDO")
    class ParseInvalido {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("bbox nulo ou vazio é rejeitado")
        void rejeitaNuloOuVazio(String bbox) {
            assertThatThrownBy(() -> BoundingBox.parse(bbox))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "-23.60,-46.70,-23.50",
                "-23.60,-46.70,-23.50,-46.60,0",
                "-23.60"
        })
        @DisplayName("quantidade de valores diferente de 4 é rejeitada")
        void rejeitaQuantidadeErrada(String bbox) {
            assertThatThrownBy(() -> BoundingBox.parse(bbox))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }

        @Test
        @DisplayName("valor não numérico é rejeitado, sem estourar NumberFormatException")
        void rejeitaValorNaoNumerico() {
            assertThatThrownBy(() -> BoundingBox.parse("a,-46.70,-23.50,-46.60"))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("invariantes de construção")
    class Invariantes {

        @Test
        @DisplayName("minLat >= maxLat é rejeitado")
        void rejeitaMinLatMaiorOuIgualMaxLat() {
            assertThatThrownBy(() -> new BoundingBox(
                    new BigDecimal("-23.50"), new BigDecimal("-46.70"), new BigDecimal("-23.50"), new BigDecimal("-46.60")))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }

        @Test
        @DisplayName("minLon >= maxLon é rejeitado")
        void rejeitaMinLonMaiorOuIgualMaxLon() {
            assertThatThrownBy(() -> new BoundingBox(
                    new BigDecimal("-23.60"), new BigDecimal("-46.60"), new BigDecimal("-23.50"), new BigDecimal("-46.60")))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }

        @Test
        @DisplayName("latitude fora de [-90, 90] é rejeitada")
        void rejeitaLatitudeForaDoIntervalo() {
            assertThatThrownBy(() -> new BoundingBox(
                    new BigDecimal("-91"), new BigDecimal("-46.70"), new BigDecimal("-23.50"), new BigDecimal("-46.60")))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }

        @Test
        @DisplayName("longitude fora de [-180, 180] é rejeitada")
        void rejeitaLongitudeForaDoIntervalo() {
            assertThatThrownBy(() -> new BoundingBox(
                    new BigDecimal("-23.60"), new BigDecimal("-181"), new BigDecimal("-23.50"), new BigDecimal("-46.60")))
                    .isInstanceOf(BoundingBoxInvalidoException.class);
        }
    }
}
