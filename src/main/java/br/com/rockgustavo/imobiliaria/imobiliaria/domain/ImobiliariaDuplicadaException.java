package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ImobiliariaDuplicadaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public static ImobiliariaDuplicadaException porCnpj(String cnpj) {
        return new ImobiliariaDuplicadaException("IMOBILIARIA_CNPJ_DUPLICADO", "CNPJ já cadastrado: " + cnpj);
    }

    public static ImobiliariaDuplicadaException porSlug(String slug) {
        return new ImobiliariaDuplicadaException("IMOBILIARIA_SLUG_DUPLICADO", "Slug já cadastrado: " + slug);
    }

    private ImobiliariaDuplicadaException(String codigo, String mensagem) {
        super(codigo, HttpStatus.CONFLICT, mensagem);
    }
}
