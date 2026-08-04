package br.com.rockgustavo.imobiliaria.orcamento.application;

import java.util.List;
import java.util.UUID;

public record AtualizarOrcamentoComando(UUID orcamentoId, List<ItemComando> itens) {
}
