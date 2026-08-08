package br.com.rockgustavo.imobiliaria.contrato.infra;

import java.time.Instant;

public record ContratoHistoricoView(int versao, String snapshot, Instant ocorridoEm) {
}
