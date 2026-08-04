package br.com.rockgustavo.imobiliaria.orcamento.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoItemRequest(
        @NotNull UUID propriedadeId,
        @NotNull @Positive @Schema(example = "6.00") BigDecimal comissaoPercentual,
        @NotNull @Positive @Schema(example = "450000.00") BigDecimal valorPedido) {
}
