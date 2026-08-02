package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;

import java.util.UUID;

public record AtribuirPapelComando(UUID pessoaId, Papel papel, String email) {
}
