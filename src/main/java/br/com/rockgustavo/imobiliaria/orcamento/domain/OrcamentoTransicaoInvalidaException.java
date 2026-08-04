package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoTransicaoInvalidaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoTransicaoInvalidaException(UUID id) {
        super("ORCAMENTO_EXPIRADO_OU_RECUSADO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Orçamento %s não está ENVIADO com validade vigente — não pode ser aceito nem recusado".formatted(id));
    }
}
