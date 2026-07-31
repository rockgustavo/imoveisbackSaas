package br.com.rockgustavo.imobiliaria.imobiliaria.application;

import java.math.BigDecimal;

public record AtualizarParametrosComando(
        BigDecimal comissaoPercentualTeto,
        Integer orcamentoValidadeDiasPadrao,
        Short geocodificacaoTentativasMax,
        Integer cepCacheJanelaDias,
        String fusoHorario
) {
}
