package br.com.rockgustavo.imobiliaria.propriedade.api;

import br.com.rockgustavo.imobiliaria.shared.geo.Coordenada;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PesquisarGeolocalizacaoResponse(
        boolean encontrada,
        @Schema(example = "-23.561684") BigDecimal latitude,
        @Schema(example = "-46.655981") BigDecimal longitude) {

    static PesquisarGeolocalizacaoResponse encontrada(Coordenada coordenada) {
        return new PesquisarGeolocalizacaoResponse(true, coordenada.latitude(), coordenada.longitude());
    }

    static PesquisarGeolocalizacaoResponse naoEncontrada() {
        return new PesquisarGeolocalizacaoResponse(false, null, null);
    }
}
