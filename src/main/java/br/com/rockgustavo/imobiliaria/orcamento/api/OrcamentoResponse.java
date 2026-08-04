package br.com.rockgustavo.imobiliaria.orcamento.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        UUID pessoaId,
        String status,
        String validade,
        UUID origemId,
        int versao,
        List<OrcamentoItemResponse> itens,
        Instant criadoEm,
        Instant alteradoEm) {
}
