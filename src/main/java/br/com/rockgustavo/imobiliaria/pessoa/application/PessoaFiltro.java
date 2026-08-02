package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.ClassificacaoComercial;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;

public record PessoaFiltro(String documento, Papel papel, ClassificacaoComercial classificacao, Boolean ativo) {
}
