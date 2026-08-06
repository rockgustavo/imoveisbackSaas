package br.com.rockgustavo.imobiliaria.propriedade.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PesquisarGeolocalizacaoRequest(
        @NotBlank @Schema(example = "01310100", description = "Só dígitos") String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String bairro,
        @NotBlank String localidade,
        @NotBlank @Size(min = 2, max = 2) @Schema(example = "SP") String uf) {
}
