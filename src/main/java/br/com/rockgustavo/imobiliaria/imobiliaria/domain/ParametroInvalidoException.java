package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ParametroInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ParametroInvalidoException(String mensagem) {
        super("TENANT_PARAMETRO_INVALIDO", HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }
}
