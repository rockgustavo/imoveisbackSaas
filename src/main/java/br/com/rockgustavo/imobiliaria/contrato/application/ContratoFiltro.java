package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;

import java.util.UUID;

public record ContratoFiltro(UUID pessoaId, StatusContrato status, Integer vencendoEmDias) {
}
