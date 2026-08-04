package br.com.rockgustavo.imobiliaria.orcamento.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrcamentoItemDetalhe(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
}
