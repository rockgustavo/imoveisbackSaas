package br.com.rockgustavo.imobiliaria.contrato.application;

import java.time.LocalDate;
import java.util.UUID;

public record CriarContratoComando(UUID orcamentoId, LocalDate vigenciaInicio, LocalDate vigenciaFim, String regrasContratuais) {
}
