package br.com.rockgustavo.imobiliaria.shared.geo;

import java.util.Optional;

public interface GeocodificacaoClient {

    Optional<Coordenada> geocodificar(EnderecoParaGeocodificar endereco);
}
