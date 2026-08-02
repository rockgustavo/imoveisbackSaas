package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PapelJaAtribuidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PapelJaAtribuidoException(Papel papel) {
        super("PESSOA_PAPEL_JA_ATRIBUIDO", HttpStatus.CONFLICT, "Papel já atribuído: " + papel);
    }
}
