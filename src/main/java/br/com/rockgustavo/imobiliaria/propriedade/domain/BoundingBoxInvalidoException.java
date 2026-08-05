package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class BoundingBoxInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BoundingBoxInvalidoException(String motivo) {
        super("BOUNDING_BOX_INVALIDO", HttpStatus.BAD_REQUEST, "Bounding box inválido: " + motivo);
    }
}
