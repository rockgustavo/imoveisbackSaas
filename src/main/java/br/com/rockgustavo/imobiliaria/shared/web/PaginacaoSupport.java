package br.com.rockgustavo.imobiliaria.shared.web;

import org.springframework.data.domain.Pageable;

public final class PaginacaoSupport {

    public static final int TAMANHO_MAXIMO = 100;

    private PaginacaoSupport() {
    }

    public static void validar(Pageable pageable) {
        if (pageable.getPageSize() > TAMANHO_MAXIMO) {
            throw new PaginacaoInvalidaException(pageable.getPageSize());
        }
    }
}
