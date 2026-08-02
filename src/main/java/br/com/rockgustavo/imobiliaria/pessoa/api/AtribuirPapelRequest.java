package br.com.rockgustavo.imobiliaria.pessoa.api;

import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AtribuirPapelRequest(@NotNull Papel papel, @Email String email) {
}
