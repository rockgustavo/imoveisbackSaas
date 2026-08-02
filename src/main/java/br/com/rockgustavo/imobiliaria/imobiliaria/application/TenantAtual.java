package br.com.rockgustavo.imobiliaria.imobiliaria.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.domain.StatusImobiliaria;

import java.util.UUID;

public record TenantAtual(UUID id, String razaoSocial, String slug, StatusImobiliaria status) {
}
