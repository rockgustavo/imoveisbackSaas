package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ContratoDetalhe(
        UUID id,
        UUID pessoaId,
        UUID orcamentoOrigemId,
        StatusContrato status,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        String regrasContratuais,
        String justificativaEncerramento,
        List<AgenciamentoDetalhe> agenciamentos,
        List<AditivoDetalhe> aditivos,
        Instant criadoEm,
        Instant alteradoEm) {
}
