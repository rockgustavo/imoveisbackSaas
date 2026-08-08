package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;

import java.util.UUID;

public record PropriedadeEnderecoView(
        UUID id,
        TipoPropriedade tipo,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf) {
}
