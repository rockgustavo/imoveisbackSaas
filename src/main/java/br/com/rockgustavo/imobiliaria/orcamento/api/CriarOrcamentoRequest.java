package br.com.rockgustavo.imobiliaria.orcamento.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CriarOrcamentoRequest(
        @NotNull UUID pessoaId,
        @NotEmpty @Valid List<OrcamentoItemRequest> itens) {
}
