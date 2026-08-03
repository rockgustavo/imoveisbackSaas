package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PropriedadeComAgenciamentoVigenteException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PropriedadeComAgenciamentoVigenteException(UUID propriedadeId) {
        super("PROPRIEDADE_COM_AGENCIAMENTO_VIGENTE", HttpStatus.UNPROCESSABLE_ENTITY,
                "Propriedade %s tem agenciamento vigente — não pode trocar de proprietário".formatted(propriedadeId));
    }
}
