package br.com.rockgustavo.imobiliaria.propriedade.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropriedadeResponse(
        UUID id,
        UUID proprietarioId,
        String tipo,
        BigDecimal areaPrivativa,
        Short quartos,
        Short vagas,
        String valorReferencia,
        String situacao,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        boolean enderecoValidado,
        BigDecimal latitude,
        BigDecimal longitude,
        String geoSituacao,
        Instant criadoEm,
        Instant alteradoEm) {
}
