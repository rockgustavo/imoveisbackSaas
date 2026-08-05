package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ContratoNaoEncontradoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoNaoEncontradoException(UUID id) {
        super("CONTRATO_NAO_ENCONTRADO", HttpStatus.NOT_FOUND, "Contrato %s não encontrado".formatted(id));
    }
}
