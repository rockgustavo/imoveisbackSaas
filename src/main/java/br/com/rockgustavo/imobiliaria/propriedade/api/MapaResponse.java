package br.com.rockgustavo.imobiliaria.propriedade.api;

import java.util.List;

public record MapaResponse(List<MapaPropriedadeResponse> propriedades, boolean limitado) {
}
