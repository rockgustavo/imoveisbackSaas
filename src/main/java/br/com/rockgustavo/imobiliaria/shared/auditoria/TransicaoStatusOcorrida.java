package br.com.rockgustavo.imobiliaria.shared.auditoria;

import java.util.UUID;

public record TransicaoStatusOcorrida(
        UUID tenantId,
        EntidadeAuditavel entidadeTipo,
        UUID entidadeId,
        String statusAnterior,
        String statusNovo,
        UUID autor) {
}
