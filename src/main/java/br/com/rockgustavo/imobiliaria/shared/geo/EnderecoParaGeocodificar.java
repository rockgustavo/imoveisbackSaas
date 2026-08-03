package br.com.rockgustavo.imobiliaria.shared.geo;

public record EnderecoParaGeocodificar(
        String cep,
        String logradouro,
        String numero,
        String bairro,
        String localidade,
        String uf) {
}
