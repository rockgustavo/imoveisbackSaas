package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PessoaInativaNaoRecebeOrcamentoException extends DomainException {

    private static final long serialVersionUID = 1L;

    public PessoaInativaNaoRecebeOrcamentoException(UUID pessoaId) {
        super("PESSOA_INATIVA_NAO_RECEBE_ORCAMENTO", HttpStatus.UNPROCESSABLE_ENTITY,
                "Pessoa %s precisa estar ativa para receber um orçamento".formatted(pessoaId));
    }
}
