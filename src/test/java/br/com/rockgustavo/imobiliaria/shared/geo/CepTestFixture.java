package br.com.rockgustavo.imobiliaria.shared.geo;

import java.util.concurrent.atomic.AtomicInteger;

public final class CepTestFixture {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);

    private CepTestFixture() {
    }

    public static String novoCep() {
        return "50%06d".formatted(SEQUENCIA.getAndIncrement());
    }
}
