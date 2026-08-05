package br.com.rockgustavo.imobiliaria.contrato.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CriarContratoRequest(
        @NotNull UUID orcamentoId,
        @NotNull @Schema(example = "2026-01-01") LocalDate vigenciaInicio,
        @NotNull @Schema(example = "2026-12-31") LocalDate vigenciaFim,
        @NotBlank String regrasContratuais) {
}
