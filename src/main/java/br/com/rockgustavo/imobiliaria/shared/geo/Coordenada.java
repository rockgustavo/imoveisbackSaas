package br.com.rockgustavo.imobiliaria.shared.geo;

import java.math.BigDecimal;

public record Coordenada(BigDecimal latitude, BigDecimal longitude) {

    private static final BigDecimal LATITUDE_MINIMA = new BigDecimal("-33.75");
    private static final BigDecimal LATITUDE_MAXIMA = new BigDecimal("5.27");
    private static final BigDecimal LONGITUDE_MINIMA = new BigDecimal("-73.99");
    private static final BigDecimal LONGITUDE_MAXIMA = new BigDecimal("-34.79");

    public boolean dentroDoTerritorioNacional() {
        return latitude != null && longitude != null
                && entre(latitude, LATITUDE_MINIMA, LATITUDE_MAXIMA)
                && entre(longitude, LONGITUDE_MINIMA, LONGITUDE_MAXIMA);
    }

    private static boolean entre(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        return valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }
}
