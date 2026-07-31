package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CnpjInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public CnpjInvalidoException(String cnpj) {
        super("IMOBILIARIA_CNPJ_INVALIDO", HttpStatus.BAD_REQUEST, "CNPJ inválido: " + cnpj);
    }
}
