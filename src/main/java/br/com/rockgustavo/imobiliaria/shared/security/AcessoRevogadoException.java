package br.com.rockgustavo.imobiliaria.shared.security;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class AcessoRevogadoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public AcessoRevogadoException() {
        super("ACESSO_REVOGADO", HttpStatus.FORBIDDEN, "Esta conta foi inativada e não tem mais acesso ao sistema");
    }
}
