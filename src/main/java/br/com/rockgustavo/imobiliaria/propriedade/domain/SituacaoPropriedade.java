package br.com.rockgustavo.imobiliaria.propriedade.domain;

import java.util.Map;
import java.util.Set;

public enum SituacaoPropriedade {
    DISPONIVEL,
    AGENCIADA,
    RESERVADA,
    VENDIDA,
    RETIRADA;

    private static final Map<SituacaoPropriedade, Set<SituacaoPropriedade>> TRANSICOES_VALIDAS = Map.of(
            DISPONIVEL, Set.of(AGENCIADA, RETIRADA),
            AGENCIADA, Set.of(DISPONIVEL, RESERVADA),
            RESERVADA, Set.of(AGENCIADA, VENDIDA),
            VENDIDA, Set.of(),
            RETIRADA, Set.of()
    );

    public boolean podeTransicionarPara(SituacaoPropriedade destino) {
        return TRANSICOES_VALIDAS.get(this).contains(destino);
    }
}
