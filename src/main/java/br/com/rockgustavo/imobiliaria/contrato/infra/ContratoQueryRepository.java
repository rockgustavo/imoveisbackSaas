package br.com.rockgustavo.imobiliaria.contrato.infra;

import br.com.rockgustavo.imobiliaria.contrato.application.ContratoFiltro;
import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContratoQueryRepository {

    private final JdbcClient jdbcClient;

    public ContratoQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Page<ContratoResumoView> listar(UUID tenantId, ContratoFiltro filtro, LocalDate hoje, Pageable pageable) {
        String condicoes = condicoesDoFiltro(filtro);
        long total = contar(tenantId, filtro, hoje, condicoes);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT c.id, c.pessoa_id, c.status, c.vigencia_inicio, c.vigencia_fim, c.criado_em,
                       count(a.id) AS quantidade_agenciamentos, coalesce(sum(a.valor_pedido), 0) AS valor_total
                  FROM contrato c
                  LEFT JOIN agenciamento a ON a.contrato_id = c.id
                 WHERE c.tenant_id = :tenantId
                """ + condicoes + """
                 GROUP BY c.id, c.pessoa_id, c.status, c.vigencia_inicio, c.vigencia_fim, c.criado_em
                 ORDER BY c.criado_em DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenantId", tenantId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset());
        consulta = aplicarParametrosDeFiltro(consulta, filtro, hoje);

        List<ContratoResumoView> conteudo = consulta.query(this::mapear).list();
        return new PageImpl<>(conteudo, pageable, total);
    }

    public List<ContratoAtivoView> buscarAtivosDeTodosOsTenants() {
        return jdbcClient.sql("""
                SELECT id, tenant_id, vigencia_fim
                  FROM contrato
                 WHERE status = 'ATIVO'
                """)
                .query(this::mapearAtivo)
                .list();
    }

    public Optional<ConflitoVigenciaView> buscarConflito(UUID tenantId, UUID propriedadeId, LocalDate inicio, LocalDate fim) {
        return jdbcClient.sql("""
                SELECT a.contrato_id, a.contrato_vigencia_inicio, a.contrato_vigencia_fim
                  FROM agenciamento a
                 WHERE a.tenant_id = :tenantId
                   AND a.propriedade_id = :propriedadeId
                   AND a.contrato_ativo = true
                   AND a.contrato_vigencia && daterange(:inicio, :fim, '[]')
                 LIMIT 1
                """)
                .param("tenantId", tenantId)
                .param("propriedadeId", propriedadeId)
                .param("inicio", inicio)
                .param("fim", fim)
                .query(this::mapearConflito)
                .optional();
    }

    private long contar(UUID tenantId, ContratoFiltro filtro, LocalDate hoje, String condicoes) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT count(*)
                  FROM contrato c
                 WHERE c.tenant_id = :tenantId
                """ + condicoes)
                .param("tenantId", tenantId);
        consulta = aplicarParametrosDeFiltro(consulta, filtro, hoje);
        return consulta.query(Long.class).single();
    }

    private String condicoesDoFiltro(ContratoFiltro filtro) {
        StringBuilder condicoes = new StringBuilder();
        if (filtro.pessoaId() != null) {
            condicoes.append(" AND c.pessoa_id = :pessoaId");
        }
        if (filtro.status() != null) {
            condicoes.append(" AND c.status = :status");
        }
        if (filtro.vencendoEmDias() != null) {
            condicoes.append(" AND c.status = 'ATIVO' AND c.vigencia_fim BETWEEN :hoje AND :hojeLimite");
        }
        return condicoes.toString();
    }

    private JdbcClient.StatementSpec aplicarParametrosDeFiltro(JdbcClient.StatementSpec consulta, ContratoFiltro filtro,
                                                                 LocalDate hoje) {
        if (filtro.pessoaId() != null) {
            consulta = consulta.param("pessoaId", filtro.pessoaId());
        }
        if (filtro.status() != null) {
            consulta = consulta.param("status", filtro.status().name());
        }
        if (filtro.vencendoEmDias() != null) {
            consulta = consulta.param("hoje", hoje).param("hojeLimite", hoje.plusDays(filtro.vencendoEmDias()));
        }
        return consulta;
    }

    private ContratoResumoView mapear(ResultSet rs, int rowNum) throws SQLException {
        return new ContratoResumoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("pessoa_id"),
                StatusContrato.valueOf(rs.getString("status")),
                rs.getDate("vigencia_inicio").toLocalDate(),
                rs.getDate("vigencia_fim").toLocalDate(),
                rs.getLong("quantidade_agenciamentos"),
                rs.getBigDecimal("valor_total"),
                rs.getTimestamp("criado_em").toInstant());
    }

    private ContratoAtivoView mapearAtivo(ResultSet rs, int rowNum) throws SQLException {
        return new ContratoAtivoView(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("tenant_id"),
                rs.getDate("vigencia_fim").toLocalDate());
    }

    private ConflitoVigenciaView mapearConflito(ResultSet rs, int rowNum) throws SQLException {
        return new ConflitoVigenciaView(
                (UUID) rs.getObject("contrato_id"),
                rs.getDate("contrato_vigencia_inicio").toLocalDate(),
                rs.getDate("contrato_vigencia_fim").toLocalDate());
    }
}
