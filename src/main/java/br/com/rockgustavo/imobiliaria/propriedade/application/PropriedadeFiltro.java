package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;

import java.math.BigDecimal;
import java.util.UUID;

public record PropriedadeFiltro(
        SituacaoPropriedade situacao,
        UUID proprietarioId,
        String localidade,
        String uf,
        BigDecimal valorMin,
        BigDecimal valorMax) {
}
