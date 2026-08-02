package br.com.rockgustavo.imobiliaria.pessoa.application;

import java.util.UUID;

public record AtualizarPessoaComando(UUID pessoaId, String nome, String email) {
}
