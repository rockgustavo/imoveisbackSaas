package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import java.util.UUID;

public record TenantResponse(UUID id, String razaoSocial, String slug, String status) {
}
