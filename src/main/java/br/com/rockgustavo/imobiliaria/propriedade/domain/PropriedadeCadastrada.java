package br.com.rockgustavo.imobiliaria.propriedade.domain;

import java.util.UUID;

public record PropriedadeCadastrada(UUID propriedadeId, UUID tenantId) {
}
