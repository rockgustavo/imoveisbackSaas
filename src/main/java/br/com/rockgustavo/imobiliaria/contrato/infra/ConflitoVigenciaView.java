package br.com.rockgustavo.imobiliaria.contrato.infra;

import java.time.LocalDate;
import java.util.UUID;

public record ConflitoVigenciaView(UUID contratoId, LocalDate vigenciaInicio, LocalDate vigenciaFim) {
}
