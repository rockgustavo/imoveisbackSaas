package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;

public class ContratoVigenciaSobrepostaException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoVigenciaSobrepostaException(UUID propriedadeId, UUID contratoConflitanteId, LocalDate inicio, LocalDate fim) {
        super("CONTRATO_VIGENCIA_SOBREPOSTA", HttpStatus.CONFLICT,
                "Propriedade %s já está agenciada pelo contrato %s entre %s e %s"
                        .formatted(propriedadeId, contratoConflitanteId, inicio, fim));
    }

    public ContratoVigenciaSobrepostaException(UUID contratoId, LocalDate inicio, LocalDate fim) {
        super("CONTRATO_VIGENCIA_SOBREPOSTA", HttpStatus.CONFLICT,
                ("Ativação do contrato %s rejeitada: alguma propriedade já está agenciada por outro contrato ativo "
                        + "com vigência sobreposta a %s–%s").formatted(contratoId, inicio, fim));
    }
}
