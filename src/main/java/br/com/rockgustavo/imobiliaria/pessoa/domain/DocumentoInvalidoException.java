package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class DocumentoInvalidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public DocumentoInvalidoException(TipoDocumento tipoDocumento, String documento) {
        super("PESSOA_DOCUMENTO_INVALIDO", HttpStatus.BAD_REQUEST,
                "%s inválido: %s".formatted(tipoDocumento, documento));
    }
}
