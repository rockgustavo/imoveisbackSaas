package br.com.rockgustavo.imobiliaria.contrato.api;

import java.time.Instant;
import java.util.UUID;

public record ContratoResumoResponse(
        UUID id,
        UUID pessoaId,
        String status,
        String vigenciaInicio,
        String vigenciaFim,
        long quantidadeAgenciamentos,
        String valorTotal,
        Instant criadoEm) {
}
