package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.domain.GeoSituacao;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.TipoPropriedade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class PropriedadeQueryRepository {

    private final JdbcClient jdbcClient;

    public PropriedadeQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Page<PropriedadeResumoView> listar(UUID tenantId, PropriedadeFiltro filtro, Pageable pageable) {
        String condicoes = condicoesDoFiltro(filtro);
        long total = contar(tenantId, filtro, condicoes);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT p.id, p.proprietario_id, p.tipo, p.valor_referencia, p.situacao,
                       p.logradouro, p.bairro, p.localidade, p.uf, p.latitude, p.longitude,
                       p.geo_situacao, p.criado_em
                  FROM propriedade p
                 WHERE p.tenant_id = :tenantId
                """ + condicoes + """
                 ORDER BY p.criado_em DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenantId", tenantId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset());
        consulta = aplicarParametrosDeFiltro(consulta, filtro);

        List<PropriedadeResumoView> conteudo = consulta.query(this::mapear).list();
        return new PageImpl<>(conteudo, pageable, total);
    }

    private long contar(UUID tenantId, PropriedadeFiltro filtro, String condicoes) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT count(*)
                  FROM propriedade p
                 WHERE p.tenant_id = :tenantId
                """ + condicoes)
                .param("tenantId", tenantId);
        consulta = aplicarParametrosDeFiltro(consulta, filtro);
        return consulta.query(Long.class).single();
    }

    private String condicoesDoFiltro(PropriedadeFiltro filtro) {
        StringBuilder condicoes = new StringBuilder();
        if (filtro.situacao() != null) {
            condicoes.append(" AND p.situacao = :situacao");
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

    private JdbcClient.StatementSpec aplicarParametrosDeFiltro(JdbcClient.StatementSpec consulta, PropriedadeFiltro filtro) {
        if (filtro.situacao() != null) {
            consulta = consulta.param("situacao", filtro.situacao().name());
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

    public List<PropriedadePendenteGeoView> buscarPendentesDeGeoDeTodosOsTenants() {
        return jdbcClient.sql("""
                SELECT id, tenant_id, geo_tentativas, alterado_em
                  FROM propriedade
                 WHERE geo_situacao = 'PENDENTE'
                """)
                .query(this::mapearPendenteGeo)
                .list();
    }

    private PropriedadePendenteGeoView mapearPendenteGeo(ResultSet rs, int rowNum) throws SQLException {
        return new PropriedadePendenteGeoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getShort("geo_tentativas"),
                rs.getTimestamp("alterado_em").toInstant());
    }

    private PropriedadeResumoView mapear(ResultSet rs, int rowNum) throws SQLException {
        return new PropriedadeResumoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("proprietario_id"),
                TipoPropriedade.valueOf(rs.getString("tipo")),
                rs.getBigDecimal("valor_referencia"),
                SituacaoPropriedade.valueOf(rs.getString("situacao")),
                rs.getString("logradouro"),
                rs.getString("bairro"),
                rs.getString("localidade"),
                rs.getString("uf"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                GeoSituacao.valueOf(rs.getString("geo_situacao")),
                rs.getTimestamp("criado_em").toInstant());
    }
}
