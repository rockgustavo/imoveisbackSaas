package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrcamentoDetalhe(
        UUID id,
        UUID pessoaId,
        StatusOrcamento status,
        LocalDate validade,
        UUID origemId,
        int versao,
        List<OrcamentoItemDetalhe> itens,
        Instant criadoEm,
        Instant alteradoEm) {
}
