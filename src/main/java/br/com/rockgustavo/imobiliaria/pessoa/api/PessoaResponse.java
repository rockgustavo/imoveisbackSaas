package br.com.rockgustavo.imobiliaria.pessoa.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PessoaResponse(
        UUID id,
        String tipoDocumento,
        String documento,
        String nome,
        String email,
        boolean ativo,
        List<String> papeis,
        String classificacao,
        Instant criadoEm,
        Instant alteradoEm) {
}
