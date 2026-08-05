package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.application.StatusContratoFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;

import java.math.BigDecimal;
import java.util.UUID;

public record MapaPropriedadeView(
        UUID id,
        UUID proprietarioId,
        SituacaoPropriedade situacao,
        BigDecimal valorReferencia,
        String logradouro,
        String localidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        StatusContratoFiltro statusContrato) {
}
