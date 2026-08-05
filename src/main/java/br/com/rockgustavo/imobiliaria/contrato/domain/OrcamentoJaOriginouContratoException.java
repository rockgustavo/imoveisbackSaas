package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoJaOriginouContratoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoJaOriginouContratoException(UUID orcamentoId) {
        super("ORCAMENTO_JA_ORIGINOU_CONTRATO", HttpStatus.CONFLICT,
                "Orçamento %s já originou um contrato".formatted(orcamentoId));
    }
}
