package br.com.rockgustavo.imobiliaria.shared.auditoria;

import java.time.Instant;
import java.util.UUID;

public record TransicaoStatusOcorrida(
        UUID tenantId,
        EntidadeAuditavel entidadeTipo,
        UUID entidadeId,
        String statusAnterior,
        String statusNovo,
        UUID autor,
        Instant ocorridoEm) {

    public TransicaoStatusOcorrida(UUID tenantId, EntidadeAuditavel entidadeTipo, UUID entidadeId,
                                    String statusAnterior, String statusNovo, UUID autor) {
        this(tenantId, entidadeTipo, entidadeId, statusAnterior, statusNovo, autor, Instant.now());
    }
}
