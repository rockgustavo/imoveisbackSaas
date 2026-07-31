package br.com.rockgustavo.imobiliaria.imobiliaria.application;

import java.math.BigDecimal;

public record ParametrosTenant(
        BigDecimal comissaoPercentualTeto,
        int orcamentoValidadeDiasPadrao,
        short geocodificacaoTentativasMax,
        int cepCacheJanelaDias,
        String fusoHorario
) {
}
