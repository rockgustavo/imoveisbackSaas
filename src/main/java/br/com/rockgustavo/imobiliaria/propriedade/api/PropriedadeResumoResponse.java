package br.com.rockgustavo.imobiliaria.propriedade.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropriedadeResumoResponse(
        UUID id,
        UUID proprietarioId,
        String tipo,
        String valorReferencia,
        String situacao,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        String geoSituacao,
        Instant criadoEm) {
}
