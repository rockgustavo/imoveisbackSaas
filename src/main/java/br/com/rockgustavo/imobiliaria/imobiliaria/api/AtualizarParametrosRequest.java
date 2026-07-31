package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AtualizarParametrosRequest(
        @NotNull @DecimalMin(value = "0.01")
        @Schema(example = "6.00")
        BigDecimal comissaoPercentualTeto,

        @NotNull @Positive Integer orcamentoValidadeDiasPadrao,
        @NotNull @Positive Short geocodificacaoTentativasMax,
        @NotNull @Positive Integer cepCacheJanelaDias,

        @NotBlank
        @Schema(example = "America/Sao_Paulo")
        String fusoHorario
) {
}
