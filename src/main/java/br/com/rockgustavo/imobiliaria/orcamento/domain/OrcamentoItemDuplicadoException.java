package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoItemDuplicadoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoItemDuplicadoException(UUID propriedadeId) {
        super("ORCAMENTO_ITEM_DUPLICADO", HttpStatus.BAD_REQUEST,
                "Propriedade %s informada mais de uma vez nos itens do orçamento".formatted(propriedadeId));
    }
}
