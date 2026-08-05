package br.com.rockgustavo.imobiliaria.contrato.application;

import java.math.BigDecimal;
import java.util.UUID;

public record AgenciamentoDetalhe(UUID id, UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido,
                                   boolean contratoAtivo) {
}
