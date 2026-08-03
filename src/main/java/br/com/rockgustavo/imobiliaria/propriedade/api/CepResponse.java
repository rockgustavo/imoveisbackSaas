package br.com.rockgustavo.imobiliaria.propriedade.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CepResponse(
        String cep,
        boolean encontrado,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        @Schema(example = "-23.561684") BigDecimal latitude,
        @Schema(example = "-46.655981") BigDecimal longitude) {
}
