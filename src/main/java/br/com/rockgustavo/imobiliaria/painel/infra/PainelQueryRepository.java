package br.com.rockgustavo.imobiliaria.painel.infra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class PainelQueryRepository {

    private static final String CASE_CLASSIFICACAO = """
            CASE
              WHEN EXISTS (SELECT 1 FROM contrato c
                            WHERE c.tenant_id = p.tenant_id AND c.pessoa_id = p.id AND c.status = 'ATIVO')
                  THEN 'CLIENTE'
              WHEN EXISTS (SELECT 1 FROM orcamento o
                            WHERE o.tenant_id = p.tenant_id AND o.pessoa_id = p.id
                              AND o.status = 'ENVIADO' AND o.validade >= :hoje)
                  THEN 'PROSPECT'
              WHEN EXISTS (SELECT 1 FROM contrato c
                            WHERE c.tenant_id = p.tenant_id AND c.pessoa_id = p.id)
                  THEN 'CLIENTE_INATIVO'
              ELSE 'LEAD'
            END""";

    private final JdbcClient jdbcClient;

    public PainelQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public PainelAgregadosView buscarAgregados(UUID tenantId, LocalDate hoje) {
        return jdbcClient.sql("""
                SELECT
                  (SELECT count(*) FROM contrato
                    WHERE tenant_id = :tenantId AND status = 'ATIVO') AS contratos_ativos,
                  (SELECT count(*) FROM contrato
                    WHERE tenant_id = :tenantId AND status = 'ATIVO'
                      AND vigencia_fim BETWEEN :hoje AND :hojeMais30) AS contratos_vencendo_em_30_dias,
                  (SELECT count(*) FROM orcamento
                    WHERE tenant_id = :tenantId AND status = 'ENVIADO' AND validade >= :hoje)
                      AS orcamentos_aguardando_resposta,
                  (SELECT round(coalesce(sum(valor_pedido * comissao_percentual / 100), 0), 2) FROM agenciamento
                    WHERE tenant_id = :tenantId AND contrato_ativo = true AND contrato_vigencia @> :hoje)
                      AS comissao_projetada
                """)
                .param("tenantId", tenantId)
                .param("hoje", hoje)
                .param("hojeMais30", hoje.plusDays(30))
                .query(this::mapearAgregados)
                .single();
    }

    public List<ImoveisPorSituacaoView> buscarImoveisPorSituacao(UUID tenantId) {
        return jdbcClient.sql("""
                SELECT situacao, count(*) AS quantidade
                  FROM propriedade
                 WHERE tenant_id = :tenantId
                 GROUP BY situacao
                """)
                .param("tenantId", tenantId)
                .query(this::mapearSituacao)
                .list();
    }

    public List<FunilClassificacaoView> buscarFunil(UUID tenantId, LocalDate hoje) {
        return jdbcClient.sql("""
                SELECT classificacao, count(*) AS quantidade FROM (
                  SELECT
                """ + CASE_CLASSIFICACAO + """
                   AS classificacao
                    FROM pessoa p
                   WHERE p.tenant_id = :tenantId
                ) t
                GROUP BY classificacao
                """)
                .param("tenantId", tenantId)
                .param("hoje", hoje)
                .query(this::mapearFunil)
                .list();
    }

    private PainelAgregadosView mapearAgregados(ResultSet rs, int rowNum) throws SQLException {
        return new PainelAgregadosView(
                rs.getLong("contratos_ativos"),
                rs.getLong("contratos_vencendo_em_30_dias"),
                rs.getLong("orcamentos_aguardando_resposta"),
                rs.getBigDecimal("comissao_projetada"));
    }

    private ImoveisPorSituacaoView mapearSituacao(ResultSet rs, int rowNum) throws SQLException {
        return new ImoveisPorSituacaoView(rs.getString("situacao"), rs.getLong("quantidade"));
    }

    private FunilClassificacaoView mapearFunil(ResultSet rs, int rowNum) throws SQLException {
        return new FunilClassificacaoView(rs.getString("classificacao"), rs.getLong("quantidade"));
    }
}
