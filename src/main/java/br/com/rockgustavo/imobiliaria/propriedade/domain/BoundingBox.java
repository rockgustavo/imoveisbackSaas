package br.com.rockgustavo.imobiliaria.propriedade.domain;

import java.math.BigDecimal;

public record BoundingBox(BigDecimal minLat, BigDecimal minLon, BigDecimal maxLat, BigDecimal maxLon) {

    private static final BigDecimal LATITUDE_MINIMA = new BigDecimal("-90");
    private static final BigDecimal LATITUDE_MAXIMA = new BigDecimal("90");
    private static final BigDecimal LONGITUDE_MINIMA = new BigDecimal("-180");
    private static final BigDecimal LONGITUDE_MAXIMA = new BigDecimal("180");

    public BoundingBox {
        exigirDentroDoIntervalo(minLat, LATITUDE_MINIMA, LATITUDE_MAXIMA);
        exigirDentroDoIntervalo(maxLat, LATITUDE_MINIMA, LATITUDE_MAXIMA);
        exigirDentroDoIntervalo(minLon, LONGITUDE_MINIMA, LONGITUDE_MAXIMA);
        exigirDentroDoIntervalo(maxLon, LONGITUDE_MINIMA, LONGITUDE_MAXIMA);
        if (minLat.compareTo(maxLat) >= 0) {
            throw new BoundingBoxInvalidoException("minLat deve ser menor que maxLat");
        }
        if (minLon.compareTo(maxLon) >= 0) {
            throw new BoundingBoxInvalidoException("minLon deve ser menor que maxLon");
        }
    }

    public static BoundingBox parse(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            throw new BoundingBoxInvalidoException("parâmetro bbox é obrigatório");
        }
        String[] partes = bbox.split(",");
        if (partes.length != 4) {
            throw new BoundingBoxInvalidoException("esperado minLat,minLon,maxLat,maxLon, recebido: " + bbox);
        }
        try {
            return new BoundingBox(
                    new BigDecimal(partes[0].trim()),
                    new BigDecimal(partes[1].trim()),
                    new BigDecimal(partes[2].trim()),
                    new BigDecimal(partes[3].trim()));
        } catch (NumberFormatException e) {
            throw new BoundingBoxInvalidoException("valor não numérico em: " + bbox);
        }
    }

    private static void exigirDentroDoIntervalo(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        if (valor.compareTo(minimo) < 0 || valor.compareTo(maximo) > 0) {
            throw new BoundingBoxInvalidoException("%s fora do intervalo [%s, %s]".formatted(valor, minimo, maximo));
        }
    }
}
