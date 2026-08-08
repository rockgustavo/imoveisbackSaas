package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;

public class ContratoHistoricoNaoEncontradoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoHistoricoNaoEncontradoException(UUID contratoId, LocalDate data) {
        super("CONTRATO_HISTORICO_NAO_ENCONTRADO", HttpStatus.NOT_FOUND,
                "Contrato %s não possui histórico registrado até %s".formatted(contratoId, data));
    }
}
