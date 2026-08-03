package br.com.rockgustavo.imobiliaria.shared.geo;

import java.math.BigDecimal;

public record CepConsulta(
        String cep,
        boolean encontrado,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude) {

    public static CepConsulta naoEncontrado(String cep) {
        return new CepConsulta(cep, false, null, null, null, null, null, null);
    }
}
