package br.com.rockgustavo.imobiliaria.painel.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record PainelIndicadoresResponse(
        long contratosAtivos,
        long contratosVencendoEm30Dias,
        @Schema(example = "{\"DISPONIVEL\":12,\"AGENCIADA\":8,\"RESERVADA\":1,\"VENDIDA\":3,\"RETIRADA\":2}")
        Map<String, Long> imoveisPorSituacao,
        long orcamentosAguardandoResposta,
        FunilResponse funil,
        @Schema(description = "Soma projetada de comissão dos agenciamentos com contrato ATIVO e vigência corrente "
                + "— é projeção, não receita realizada (RN-08-03)", example = "125000.00")
        String comissaoProjetada) {
}
