package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;

import java.util.UUID;

public record OrcamentoFiltro(UUID pessoaId, StatusOrcamento status) {
}
