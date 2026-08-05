package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ContratoTransicaoInvalidaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoTransicaoInvalidaException(UUID id, StatusContrato statusAtual) {
        super("CONTRATO_TRANSICAO_INVALIDA", HttpStatus.UNPROCESSABLE_ENTITY,
                "Contrato %s está em %s e não admite essa operação".formatted(id, statusAtual));
    }
}
