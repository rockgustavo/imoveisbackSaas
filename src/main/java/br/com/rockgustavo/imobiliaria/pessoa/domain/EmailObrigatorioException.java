package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class EmailObrigatorioException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EmailObrigatorioException(Papel papel) {
        super("PESSOA_EMAIL_OBRIGATORIO", HttpStatus.UNPROCESSABLE_ENTITY,
                "E-mail é obrigatório para atribuir o papel " + papel);
    }
}
