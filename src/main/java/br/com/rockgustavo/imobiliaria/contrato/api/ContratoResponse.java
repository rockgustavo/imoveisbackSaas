package br.com.rockgustavo.imobiliaria.contrato.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContratoResponse(
        UUID id,
        UUID pessoaId,
        UUID orcamentoOrigemId,
        String status,
        String vigenciaInicio,
        String vigenciaFim,
        String regrasContratuais,
        String justificativaEncerramento,
        List<AgenciamentoResponse> agenciamentos,
        List<AditivoResponse> aditivos,
        Instant criadoEm,
        Instant alteradoEm) {
}
