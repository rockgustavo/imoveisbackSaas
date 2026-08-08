package br.com.rockgustavo.imobiliaria.contrato.application;

import java.time.Instant;

public record ContratoHistoricoDetalhe(int versao, Instant ocorridoEm, ContratoDetalhe contrato) {
}
