package br.com.rockgustavo.imobiliaria.contrato.domain;

import java.util.Map;
import java.util.Set;

public enum StatusContrato {
    RASCUNHO,
    ATIVO,
    ENCERRADO,
    CANCELADO,
    EXPIRADO;

    private static final Map<StatusContrato, Set<StatusContrato>> TRANSICOES_VALIDAS = Map.of(
            RASCUNHO, Set.of(ATIVO, CANCELADO),
            ATIVO, Set.of(ENCERRADO, CANCELADO, EXPIRADO),
            ENCERRADO, Set.of(),
            CANCELADO, Set.of(),
            EXPIRADO, Set.of()
    );

    public boolean podeTransicionarPara(StatusContrato destino) {
        return TRANSICOES_VALIDAS.get(this).contains(destino);
    }
}
