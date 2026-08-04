package br.com.rockgustavo.imobiliaria.orcamento.domain;

import java.util.Map;
import java.util.Set;

public enum StatusOrcamento {
    RASCUNHO,
    ENVIADO,
    ACEITO,
    RECUSADO,
    EXPIRADO;

    private static final Map<StatusOrcamento, Set<StatusOrcamento>> TRANSICOES_VALIDAS = Map.of(
            RASCUNHO, Set.of(ENVIADO),
            ENVIADO, Set.of(ACEITO, RECUSADO, EXPIRADO),
            ACEITO, Set.of(),
            RECUSADO, Set.of(),
            EXPIRADO, Set.of()
    );

    public boolean podeTransicionarPara(StatusOrcamento destino) {
        return TRANSICOES_VALIDAS.get(this).contains(destino);
    }
}
