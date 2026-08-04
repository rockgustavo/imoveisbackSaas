package br.com.rockgustavo.imobiliaria.orcamento.application;

import java.util.List;
import java.util.UUID;

public record CriarOrcamentoComando(UUID pessoaId, List<ItemComando> itens) {
}
