package br.com.rockgustavo.imobiliaria.contrato.api;

import jakarta.validation.constraints.NotBlank;

public record EncerramentoRequest(@NotBlank String justificativa) {
}
