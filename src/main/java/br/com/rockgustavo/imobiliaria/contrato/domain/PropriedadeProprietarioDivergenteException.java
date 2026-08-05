package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PropriedadeProprietarioDivergenteException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PropriedadeProprietarioDivergenteException(UUID propriedadeId, UUID proprietarioEsperado) {
        super("PROPRIEDADE_PROPRIETARIO_DIVERGENTE", HttpStatus.UNPROCESSABLE_ENTITY,
                "Propriedade %s não pertence ao proprietário %s do contrato".formatted(propriedadeId, proprietarioEsperado));
    }
}
