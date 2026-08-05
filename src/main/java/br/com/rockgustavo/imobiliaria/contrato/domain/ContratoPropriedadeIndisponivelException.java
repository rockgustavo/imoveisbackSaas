package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ContratoPropriedadeIndisponivelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoPropriedadeIndisponivelException(UUID propriedadeId) {
        super("CONTRATO_PROPRIEDADE_INDISPONIVEL", HttpStatus.UNPROCESSABLE_ENTITY,
                "Propriedade %s precisa pertencer ao proprietário do contrato e estar DISPONIVEL para ativação"
                        .formatted(propriedadeId));
    }
}
