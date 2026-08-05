package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoNaoAceitoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoNaoAceitoException(UUID id) {
        super("ORCAMENTO_NAO_ACEITO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Orçamento %s não está ACEITO — só um orçamento aceito pode originar contrato".formatted(id));
    }
}
