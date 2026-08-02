package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DocumentoDuplicadoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DocumentoDuplicadoException(TipoDocumento tipoDocumento, String documento) {
        super("PESSOA_DOCUMENTO_DUPLICADO", HttpStatus.CONFLICT,
                "%s já cadastrado neste tenant: %s".formatted(tipoDocumento, documento));
    }
}
