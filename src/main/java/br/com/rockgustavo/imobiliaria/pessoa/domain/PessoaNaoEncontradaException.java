package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PessoaNaoEncontradaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PessoaNaoEncontradaException(UUID id) {
        super("PESSOA_NAO_ENCONTRADA", HttpStatus.NOT_FOUND, "Pessoa não encontrada: " + id);
    }
}
