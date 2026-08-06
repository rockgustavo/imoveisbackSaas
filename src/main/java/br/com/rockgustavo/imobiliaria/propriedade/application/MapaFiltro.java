package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.BoundingBox;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MapaFiltro(
        BoundingBox bbox,
        List<SituacaoPropriedade> situacoes,
        StatusContratoFiltro statusContrato,
        BigDecimal valorMin,
        BigDecimal valorMax,
        String localidade,
        String uf,
        UUID proprietarioId) {

    public MapaFiltro comSituacaoDefaultSeAusente(SituacaoPropriedade situacaoDefault) {
        if (situacoes != null && !situacoes.isEmpty()) {
            return this;
        }
        return new MapaFiltro(bbox, List.of(situacaoDefault), statusContrato, valorMin, valorMax, localidade, uf,
                proprietarioId);
    }
}
