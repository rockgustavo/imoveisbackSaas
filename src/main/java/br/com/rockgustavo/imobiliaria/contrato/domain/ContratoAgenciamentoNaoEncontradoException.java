package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ContratoAgenciamentoNaoEncontradoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoAgenciamentoNaoEncontradoException(UUID contratoId, UUID propriedadeId) {
        super("CONTRATO_AGENCIAMENTO_NAO_ENCONTRADO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Contrato %s não tem agenciamento ativo para a propriedade %s".formatted(contratoId, propriedadeId));
    }
}
