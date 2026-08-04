package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoPropriedadeIndisponivelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoPropriedadeIndisponivelException(UUID propriedadeId) {
        super("ORCAMENTO_PROPRIEDADE_INDISPONIVEL", HttpStatus.UNPROCESSABLE_ENTITY,
                "Propriedade %s precisa existir, pertencer ao proprietário do orçamento e estar DISPONIVEL"
                        .formatted(propriedadeId));
    }
}
