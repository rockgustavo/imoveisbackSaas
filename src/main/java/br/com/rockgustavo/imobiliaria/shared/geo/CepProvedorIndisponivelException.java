package br.com.rockgustavo.imobiliaria.shared.geo;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CepProvedorIndisponivelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public CepProvedorIndisponivelException(String cep, Throwable causa) {
        super("CEP_PROVEDOR_INDISPONIVEL", HttpStatus.BAD_GATEWAY,
                "Falha ao consultar o CEP " + cep + " no fornecedor externo", causa);
    }
}
