package br.com.rockgustavo.imobiliaria.contrato.infra;

import java.time.LocalDate;
import java.util.UUID;

public record ContratoAtivoView(UUID id, UUID tenantId, LocalDate vigenciaFim) {
}
