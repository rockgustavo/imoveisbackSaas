package br.com.rockgustavo.imobiliaria.contrato.api;

import java.util.UUID;

public record AditivoResponse(UUID propriedadeId, String tipo, String justificativa, String data) {
}
