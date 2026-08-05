package br.com.rockgustavo.imobiliaria.propriedade.api;

import java.math.BigDecimal;
import java.util.UUID;

public record MapaPropriedadeResponse(
        UUID id,
        UUID proprietarioId,
        String situacao,
        String valorReferencia,
        String logradouro,
        String localidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        String statusContrato) {
}
