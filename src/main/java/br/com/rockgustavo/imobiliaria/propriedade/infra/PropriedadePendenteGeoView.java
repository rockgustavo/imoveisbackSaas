package br.com.rockgustavo.imobiliaria.propriedade.infra;

import java.time.Instant;
import java.util.UUID;

public record PropriedadePendenteGeoView(UUID id, UUID tenantId, short geoTentativas, Instant alteradoEm) {
}
