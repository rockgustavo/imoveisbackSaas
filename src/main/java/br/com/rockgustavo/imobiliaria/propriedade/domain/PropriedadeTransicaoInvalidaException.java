package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PropriedadeTransicaoInvalidaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PropriedadeTransicaoInvalidaException(SituacaoPropriedade origem, SituacaoPropriedade destino) {
        super("PROPRIEDADE_TRANSICAO_INVALIDA", HttpStatus.UNPROCESSABLE_ENTITY,
                "Transição de %s para %s não é permitida".formatted(origem, destino));
    }
}
