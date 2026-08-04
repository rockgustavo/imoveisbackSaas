package br.com.rockgustavo.imobiliaria.orcamento.api;

import java.util.UUID;

public record OrcamentoItemResponse(UUID propriedadeId, String comissaoPercentual, String valorPedido) {
}
