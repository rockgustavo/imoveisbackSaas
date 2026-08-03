package br.com.rockgustavo.imobiliaria.pessoa.application;

import java.util.UUID;

public interface KeycloakAdminPort {

    String provisionar(String email, String nome, UUID tenantId);
}
