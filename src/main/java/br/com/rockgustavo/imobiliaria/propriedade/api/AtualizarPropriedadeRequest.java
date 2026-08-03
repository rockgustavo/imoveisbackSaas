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

public record AtualizarPropriedadeRequest(
        @NotNull UUID proprietarioId,
        @NotNull TipoPropriedade tipo,
        @Positive BigDecimal areaPrivativa,
        @PositiveOrZero Short quartos,
        @PositiveOrZero Short vagas,
        @NotNull @Positive BigDecimal valorReferencia,
        @NotBlank @Schema(example = "01310100", description = "Só dígitos") String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String complemento,
        @NotBlank String bairro,
        @NotBlank String localidade,
        @NotBlank @Size(min = 2, max = 2) @Schema(example = "SP") String uf,
        @NotNull Boolean enderecoValidado) {
}
