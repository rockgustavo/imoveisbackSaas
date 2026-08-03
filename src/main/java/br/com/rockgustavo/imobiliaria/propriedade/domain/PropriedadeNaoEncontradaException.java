package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PropriedadeNaoEncontradaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PropriedadeNaoEncontradaException(UUID id) {
        super("PROPRIEDADE_NAO_ENCONTRADA", HttpStatus.NOT_FOUND, "Propriedade não encontrada: " + id);
    }
}
