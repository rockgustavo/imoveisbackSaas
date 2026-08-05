package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ContratoCancelamentoInviavelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoCancelamentoInviavelException(UUID contratoId, UUID propriedadeId) {
        super("CONTRATO_CANCELAMENTO_INVIAVEL", HttpStatus.UNPROCESSABLE_ENTITY,
                "Contrato %s não pode ser cancelado: propriedade %s está RESERVADA ou VENDIDA"
                        .formatted(contratoId, propriedadeId));
    }
}
