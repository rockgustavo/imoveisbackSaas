package br.com.rockgustavo.imobiliaria.propriedade.domain;

import java.util.UUID;

public record EnderecoAlterado(UUID propriedadeId, UUID tenantId) {
}
