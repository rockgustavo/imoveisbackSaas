package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.infra.MapaPropriedadeView;

import java.util.List;

public record MapaResultado(List<MapaPropriedadeView> propriedades, boolean limitado) {
}
