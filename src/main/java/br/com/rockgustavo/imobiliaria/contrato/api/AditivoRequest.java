package br.com.rockgustavo.imobiliaria.contrato.api;

import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AditivoRequest(
        @NotNull TipoAditivo tipo,
        @NotNull UUID propriedadeId,
        @NotBlank String justificativa,
        @Schema(example = "6.00") BigDecimal comissaoPercentual,
        @Schema(example = "450000.00") BigDecimal valorPedido) {

    @AssertTrue(message = "comissaoPercentual e valorPedido são obrigatórios quando tipo=INCLUSAO")
    public boolean isCamposDeInclusaoPreenchidos() {
        return tipo != TipoAditivo.INCLUSAO || (comissaoPercentual != null && valorPedido != null);
    }
}
