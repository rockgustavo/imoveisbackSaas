package br.com.rockgustavo.imobiliaria.orcamento.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemComando(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
}
