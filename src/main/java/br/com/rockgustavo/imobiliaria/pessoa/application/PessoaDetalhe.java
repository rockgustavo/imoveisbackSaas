package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.ClassificacaoComercial;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PessoaDetalhe(
        UUID id,
        TipoDocumento tipoDocumento,
        String documento,
        String nome,
        String email,
        boolean ativo,
        List<Papel> papeis,
        ClassificacaoComercial classificacao,
        Instant criadoEm,
        Instant alteradoEm) {
}
