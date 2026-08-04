package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrcamentoNaoEditavelException extends DomainException {

    private static final long serialVersionUID = 1L;

    public OrcamentoNaoEditavelException(UUID id) {
        super("ORCAMENTO_NAO_EDITAVEL", HttpStatus.UNPROCESSABLE_ENTITY,
                "Orçamento %s só é editável em RASCUNHO — duplique para propor uma nova versão".formatted(id));
    }
}
