package br.com.rockgustavo.imobiliaria.orcamento.infra;

import java.time.LocalDate;
import java.util.UUID;

public record OrcamentoEnviadoView(UUID id, UUID tenantId, LocalDate validade) {
}
