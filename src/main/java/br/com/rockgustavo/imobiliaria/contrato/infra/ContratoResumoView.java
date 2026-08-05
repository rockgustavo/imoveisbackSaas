package br.com.rockgustavo.imobiliaria.contrato.infra;

import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContratoResumoView(
        UUID id,
        UUID pessoaId,
        StatusContrato status,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        long quantidadeAgenciamentos,
        BigDecimal valorTotal,
        Instant criadoEm) {
}
