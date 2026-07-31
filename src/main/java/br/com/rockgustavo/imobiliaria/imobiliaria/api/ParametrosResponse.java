package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record ParametrosResponse(
        @Schema(example = "6.00", description = "Percentual — string para não perder precisão")
        String comissaoPercentualTeto,

        int orcamentoValidadeDiasPadrao,
        short geocodificacaoTentativasMax,
        int cepCacheJanelaDias,

        @Schema(example = "America/Sao_Paulo")
        String fusoHorario
) {
}
