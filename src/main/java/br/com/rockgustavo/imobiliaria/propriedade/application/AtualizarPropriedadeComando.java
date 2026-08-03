package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;

import java.math.BigDecimal;
import java.util.UUID;

public record AtualizarPropriedadeComando(
        UUID propriedadeId,
        UUID proprietarioId,
        TipoPropriedade tipo,
        BigDecimal areaPrivativa,
        Short quartos,
        Short vagas,
        BigDecimal valorReferencia,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        boolean enderecoValidado) {
}
