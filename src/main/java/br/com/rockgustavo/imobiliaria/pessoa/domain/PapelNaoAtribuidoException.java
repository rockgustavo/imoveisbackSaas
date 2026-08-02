package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PapelNaoAtribuidoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PapelNaoAtribuidoException(Papel papel) {
        super("PESSOA_PAPEL_NAO_ATRIBUIDO", HttpStatus.UNPROCESSABLE_ENTITY, "Papel não atribuído: " + papel);
    }
}
