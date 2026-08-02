package br.com.rockgustavo.imobiliaria.pessoa.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AtualizarPessoaRequest(@NotBlank String nome, @Email String email) {
}
