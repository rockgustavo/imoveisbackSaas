package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class UltimoAdministradorException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UltimoAdministradorException() {
        super("PESSOA_ULTIMO_ADMINISTRADOR", HttpStatus.UNPROCESSABLE_ENTITY,
                "O tenant ficaria sem administrador — último ADMINISTRADOR ativo não pode ser removido nem inativado");
    }
}
