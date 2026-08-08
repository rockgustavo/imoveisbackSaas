package br.com.rockgustavo.imobiliaria.contrato.api;

import java.time.Instant;

public record ContratoHistoricoResponse(int versao, Instant ocorridoEm, ContratoResponse contrato) {
}
