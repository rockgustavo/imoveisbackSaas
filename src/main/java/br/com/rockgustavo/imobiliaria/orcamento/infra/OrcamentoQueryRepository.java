package br.com.rockgustavo.imobiliaria.orcamento.infra;

import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoFiltro;
import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;
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
public class OrcamentoQueryRepository {

    private final JdbcClient jdbcClient;

    public OrcamentoQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Page<OrcamentoResumoView> listar(UUID tenantId, OrcamentoFiltro filtro, Pageable pageable) {
        String condicoes = condicoesDoFiltro(filtro);
        long total = contar(tenantId, filtro, condicoes);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT o.id, o.pessoa_id, o.status, o.validade, o.versao, o.criado_em,
                       count(oi.id) AS quantidade_itens, coalesce(sum(oi.valor_pedido), 0) AS valor_total
                  FROM orcamento o
                  LEFT JOIN orcamento_item oi ON oi.orcamento_id = o.id
                 WHERE o.tenant_id = :tenantId
                """ + condicoes + """
                 GROUP BY o.id, o.pessoa_id, o.status, o.validade, o.versao, o.criado_em
                 ORDER BY o.criado_em DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenantId", tenantId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset());
        consulta = aplicarParametrosDeFiltro(consulta, filtro);

        List<OrcamentoResumoView> conteudo = consulta.query(this::mapear).list();
        return new PageImpl<>(conteudo, pageable, total);
    }

    public List<OrcamentoEnviadoView> buscarEnviadosDeTodosOsTenants() {
        return jdbcClient.sql("""
                SELECT id, tenant_id, validade
                  FROM orcamento
                 WHERE status = 'ENVIADO'
                """)
                .query(this::mapearEnviado)
                .list();
    }

    private long contar(UUID tenantId, OrcamentoFiltro filtro, String condicoes) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT count(*)
                  FROM orcamento o
                 WHERE o.tenant_id = :tenantId
                """ + condicoes)
                .param("tenantId", tenantId);
        consulta = aplicarParametrosDeFiltro(consulta, filtro);
        return consulta.query(Long.class).single();
    }

    private String condicoesDoFiltro(OrcamentoFiltro filtro) {
        StringBuilder condicoes = new StringBuilder();
        if (filtro.pessoaId() != null) {
            condicoes.append(" AND o.pessoa_id = :pessoaId");
        }
        if (filtro.status() != null) {
            condicoes.append(" AND o.status = :status");
        }
        return condicoes.toString();
    }

    private JdbcClient.StatementSpec aplicarParametrosDeFiltro(JdbcClient.StatementSpec consulta, OrcamentoFiltro filtro) {
        if (filtro.pessoaId() != null) {
            consulta = consulta.param("pessoaId", filtro.pessoaId());
        }
        if (filtro.status() != null) {
            consulta = consulta.param("status", filtro.status().name());
        }
        return consulta;
    }

    private OrcamentoResumoView mapear(ResultSet rs, int rowNum) throws SQLException {
        return new OrcamentoResumoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("pessoa_id"),
                StatusOrcamento.valueOf(rs.getString("status")),
                rs.getDate("validade").toLocalDate(),
                rs.getInt("versao"),
                rs.getLong("quantidade_itens"),
                rs.getBigDecimal("valor_total"),
                rs.getTimestamp("criado_em").toInstant());
    }

    private OrcamentoEnviadoView mapearEnviado(ResultSet rs, int rowNum) throws SQLException {
        return new OrcamentoEnviadoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getDate("validade").toLocalDate());
    }
}
