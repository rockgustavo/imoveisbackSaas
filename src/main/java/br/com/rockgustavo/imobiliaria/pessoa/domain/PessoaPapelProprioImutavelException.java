package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PessoaPapelProprioImutavelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PessoaPapelProprioImutavelException() {
        super("PESSOA_PAPEL_PROPRIO_IMUTAVEL", HttpStatus.UNPROCESSABLE_ENTITY,
                "Um usuário não pode alterar os próprios papéis");
    }
}
