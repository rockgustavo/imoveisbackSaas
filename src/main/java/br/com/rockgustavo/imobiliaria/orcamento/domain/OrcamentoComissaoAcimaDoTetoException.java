package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class OrcamentoComissaoAcimaDoTetoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoComissaoAcimaDoTetoException(BigDecimal comissaoPercentual, BigDecimal teto) {
        super("ORCAMENTO_COMISSAO_ACIMA_DO_TETO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Comissão %s%% acima do teto do tenant (%s%%)".formatted(comissaoPercentual, teto));
    }
}
