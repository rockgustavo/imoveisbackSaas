package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class SlugInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public SlugInvalidoException(String slug) {
        super("IMOBILIARIA_SLUG_INVALIDO", HttpStatus.BAD_REQUEST,
                "Slug inválido — use letras minúsculas, números e hífen: " + slug);
    }
}
