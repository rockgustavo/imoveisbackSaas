package br.com.rockgustavo.imobiliaria.orcamento.api;

import java.time.Instant;
import java.util.UUID;

public record OrcamentoResumoResponse(
        UUID id,
        UUID pessoaId,
        String status,
        String validade,
        int versao,
        long quantidadeItens,
        String valorTotal,
        Instant criadoEm) {
}
