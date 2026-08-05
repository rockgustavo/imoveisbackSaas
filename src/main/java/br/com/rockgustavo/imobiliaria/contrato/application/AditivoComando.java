package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;

import java.math.BigDecimal;
import java.util.UUID;

public record AditivoComando(UUID contratoId, TipoAditivo tipo, UUID propriedadeId, String justificativa,
                              BigDecimal comissaoPercentual, BigDecimal valorPedido) {
}
