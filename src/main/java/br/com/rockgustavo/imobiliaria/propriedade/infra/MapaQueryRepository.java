package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.application.MapaFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.application.StatusContratoFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class MapaQueryRepository {

    private final JdbcClient jdbcClient;

    public MapaQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<MapaPropriedadeView> buscar(UUID tenantId, MapaFiltro filtro, int limite) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT p.id, p.proprietario_id, p.situacao, p.valor_referencia,
                       p.logradouro, p.localidade, p.uf, p.latitude, p.longitude,
                       ultimo_agenciamento.status AS status_contrato
                  FROM propriedade p
                  LEFT JOIN LATERAL (
                         SELECT c.status
                           FROM agenciamento a
                           JOIN contrato c ON c.id = a.contrato_id
                          WHERE a.tenant_id = p.tenant_id
                            AND a.propriedade_id = p.id
                          ORDER BY a.contrato_ativo DESC, a.criado_em DESC
                          LIMIT 1
                       ) ultimo_agenciamento ON true
                 WHERE p.tenant_id = :tenantId
                   AND p.geo_situacao = 'CONCLUIDA'
                   AND p.latitude BETWEEN :minLat AND :maxLat
                   AND p.longitude BETWEEN :minLon AND :maxLon
                """ + condicoesDoFiltro(filtro) + """
                 ORDER BY p.criado_em DESC
                 LIMIT :limite
                """)
                .param("tenantId", tenantId)
                .param("minLat", filtro.bbox().minLat())
                .param("maxLat", filtro.bbox().maxLat())
                .param("minLon", filtro.bbox().minLon())
                .param("maxLon", filtro.bbox().maxLon())
                .param("limite", limite);
        consulta = aplicarParametrosDeFiltro(consulta, filtro);

        return consulta.query(this::mapear).list();
    }

    private String condicoesDoFiltro(MapaFiltro filtro) {
        StringBuilder condicoes = new StringBuilder();
        if (filtro.situacoes() != null && !filtro.situacoes().isEmpty()) {
            condicoes.append(" AND p.situacao IN (:situacoes)");
        }
        if (filtro.statusContrato() != null) {
            condicoes.append(" AND ultimo_agenciamento.status = :statusContrato");
        }
        if (filtro.proprietarioId() != null) {
            condicoes.append(" AND p.proprietario_id = :proprietarioId");
        }
        if (filtro.localidade() != null) {
            condicoes.append(" AND p.localidade = :localidade");
        }
        if (filtro.uf() != null) {
            condicoes.append(" AND p.uf = :uf");
        }
        if (filtro.valorMin() != null) {
            condicoes.append(" AND p.valor_referencia >= :valorMin");
        }
        if (filtro.valorMax() != null) {
            condicoes.append(" AND p.valor_referencia <= :valorMax");
        }
        return condicoes.toString();
    }

    private JdbcClient.StatementSpec aplicarParametrosDeFiltro(JdbcClient.StatementSpec consulta, MapaFiltro filtro) {
        if (filtro.situacoes() != null && !filtro.situacoes().isEmpty()) {
            consulta = consulta.param("situacoes", filtro.situacoes().stream().map(Enum::name).toList());
        }
        if (filtro.statusContrato() != null) {
            consulta = consulta.param("statusContrato", filtro.statusContrato().name());
        }
        if (filtro.proprietarioId() != null) {
            consulta = consulta.param("proprietarioId", filtro.proprietarioId());
        }
        if (filtro.localidade() != null) {
            consulta = consulta.param("localidade", filtro.localidade());
        }
        if (filtro.uf() != null) {
            consulta = consulta.param("uf", filtro.uf());
        }
        if (filtro.valorMin() != null) {
            consulta = consulta.param("valorMin", filtro.valorMin());
        }
        if (filtro.valorMax() != null) {
            consulta = consulta.param("valorMax", filtro.valorMax());
        }
        return consulta;
    }

    private MapaPropriedadeView mapear(ResultSet rs, int rowNum) throws SQLException {
        String statusContrato = rs.getString("status_contrato");
        return new MapaPropriedadeView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("proprietario_id"),
                SituacaoPropriedade.valueOf(rs.getString("situacao")),
                rs.getBigDecimal("valor_referencia"),
                rs.getString("logradouro"),
                rs.getString("localidade"),
                rs.getString("uf"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                statusContrato == null ? null : StatusContratoFiltro.valueOf(statusContrato));
    }
}
