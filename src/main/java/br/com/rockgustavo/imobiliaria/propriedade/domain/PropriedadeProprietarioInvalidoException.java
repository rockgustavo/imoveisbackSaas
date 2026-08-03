package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PropriedadeProprietarioInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PropriedadeProprietarioInvalidoException(UUID pessoaId) {
        super("PROPRIEDADE_PROPRIETARIO_INVALIDO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Pessoa %s precisa estar ativa e ter papel PROPRIETARIO".formatted(pessoaId));
    }
}
