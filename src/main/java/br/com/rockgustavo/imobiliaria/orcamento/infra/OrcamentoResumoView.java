package br.com.rockgustavo.imobiliaria.orcamento.infra;

import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrcamentoResumoView(
        UUID id,
        UUID pessoaId,
        StatusOrcamento status,
        LocalDate validade,
        int versao,
        long quantidadeItens,
        BigDecimal valorTotal,
        Instant criadoEm) {
}
