package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class EmailDuplicadoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EmailDuplicadoException(String email) {
        super("PESSOA_EMAIL_DUPLICADO", HttpStatus.CONFLICT, "E-mail já cadastrado neste tenant: " + email);
    }
}
