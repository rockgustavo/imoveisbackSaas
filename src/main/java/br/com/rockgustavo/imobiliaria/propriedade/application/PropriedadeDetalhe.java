package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.GeoSituacao;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropriedadeDetalhe(
        UUID id,
        UUID proprietarioId,
        TipoPropriedade tipo,
        BigDecimal areaPrivativa,
        Short quartos,
        Short vagas,
        BigDecimal valorReferencia,
        SituacaoPropriedade situacao,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        boolean enderecoValidado,
        BigDecimal latitude,
        BigDecimal longitude,
        GeoSituacao geoSituacao,
        Instant criadoEm,
        Instant alteradoEm) {
}
