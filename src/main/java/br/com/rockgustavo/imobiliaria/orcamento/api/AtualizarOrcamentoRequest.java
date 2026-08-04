package br.com.rockgustavo.imobiliaria.orcamento.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AtualizarOrcamentoRequest(@NotEmpty @Valid List<OrcamentoItemRequest> itens) {
}
