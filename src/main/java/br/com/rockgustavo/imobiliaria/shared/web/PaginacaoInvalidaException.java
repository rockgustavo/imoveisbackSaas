package br.com.rockgustavo.imobiliaria.shared.web;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class PaginacaoInvalidaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PaginacaoInvalidaException(int tamanhoSolicitado) {
        super("PAGINACAO_TAMANHO_INVALIDO", HttpStatus.BAD_REQUEST,
                "Tamanho de página inválido: %d (máximo %d)".formatted(tamanhoSolicitado, PaginacaoSupport.TAMANHO_MAXIMO));
    }
}
