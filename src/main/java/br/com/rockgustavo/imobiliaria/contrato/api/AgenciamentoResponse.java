package br.com.rockgustavo.imobiliaria.contrato.api;

import java.util.UUID;

public record AgenciamentoResponse(UUID id, UUID propriedadeId, String comissaoPercentual, String valorPedido,
                                    boolean contratoAtivo) {
}
