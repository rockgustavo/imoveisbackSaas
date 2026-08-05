package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class ContratoVigenciaInvalidaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoVigenciaInvalidaException(LocalDate inicio, LocalDate fim) {
        super("CONTRATO_VIGENCIA_INVALIDA", HttpStatus.BAD_REQUEST,
                "Vigência inválida: fim (%s) deve ser posterior ao início (%s)".formatted(fim, inicio));
    }
}
