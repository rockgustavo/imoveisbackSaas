package br.com.rockgustavo.imobiliaria.propriedade.api;

import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CriarPropriedadeRequest(
        @NotNull UUID proprietarioId,
        @NotNull TipoPropriedade tipo,
        @Positive @Schema(example = "85.50") BigDecimal areaPrivativa,
        @PositiveOrZero Short quartos,
        @PositiveOrZero Short vagas,
        @NotNull @Positive @Schema(example = "450000.00") BigDecimal valorReferencia,
        @NotBlank @Schema(example = "01310100", description = "Só dígitos") String cep,
        @NotBlank @Schema(example = "Av. Paulista") String logradouro,
        @NotBlank @Schema(example = "1000") String numero,
        String complemento,
        @NotBlank @Schema(example = "Bela Vista") String bairro,
        @NotBlank @Schema(example = "São Paulo") String localidade,
        @NotBlank @Size(min = 2, max = 2) @Schema(example = "SP") String uf,
        @NotNull @Schema(description = "false quando o CEP não foi encontrado na consulta feita por quem chamou")
        Boolean enderecoValidado) {
}
