package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;

public record CriarPessoaComando(TipoDocumento tipoDocumento, String documento, String nome, String email) {
}
