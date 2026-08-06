package br.com.rockgustavo.imobiliaria.painel.infra;

import java.math.BigDecimal;

public record PainelAgregadosView(
        long contratosAtivos,
        long contratosVencendoEm30Dias,
        long orcamentosAguardandoResposta,
        BigDecimal comissaoProjetada) {
}
