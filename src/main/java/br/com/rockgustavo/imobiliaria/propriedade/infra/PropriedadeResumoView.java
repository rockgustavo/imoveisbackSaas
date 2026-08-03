package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.domain.GeoSituacao;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropriedadeResumoView(
        UUID id,
        UUID proprietarioId,
        TipoPropriedade tipo,
        BigDecimal valorReferencia,
        SituacaoPropriedade situacao,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        BigDecimal latitude,
        BigDecimal longitude,
        GeoSituacao geoSituacao,
        Instant criadoEm) {
}
