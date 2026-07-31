package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CriarImobiliariaRequest(
        @NotBlank
        @Schema(example = "Corretora Exemplo Ltda")
        String razaoSocial,

        @NotBlank
        @Schema(example = "11.222.333/0001-81", description = "Com ou sem máscara")
        String cnpj,

        @NotBlank
        @Schema(example = "corretora-exemplo", description = "Minúsculo, sem espaço, usado em URL administrativa")
        String slug
) {
}
