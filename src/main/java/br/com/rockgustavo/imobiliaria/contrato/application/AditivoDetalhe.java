package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;

import java.time.LocalDate;
import java.util.UUID;

public record AditivoDetalhe(UUID propriedadeId, TipoAditivo tipo, String justificativa, LocalDate data) {
}
