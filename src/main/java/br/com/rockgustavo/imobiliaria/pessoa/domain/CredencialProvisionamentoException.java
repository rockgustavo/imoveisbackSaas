package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CredencialProvisionamentoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public CredencialProvisionamentoException(String email, Throwable causa) {
        super("CREDENCIAL_PROVISIONAMENTO_FALHOU", HttpStatus.BAD_GATEWAY,
                "Falha ao provisionar credencial no Keycloak" + (email != null ? " para " + email : ""), causa);
    }
}
