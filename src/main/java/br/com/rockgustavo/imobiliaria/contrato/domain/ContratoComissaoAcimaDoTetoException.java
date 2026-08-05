package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class ContratoComissaoAcimaDoTetoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ContratoComissaoAcimaDoTetoException(BigDecimal comissaoPercentual, BigDecimal teto) {
        super("CONTRATO_COMISSAO_ACIMA_DO_TETO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Comissão %s%% acima do teto do tenant (%s%%)".formatted(comissaoPercentual, teto));
    }
}
