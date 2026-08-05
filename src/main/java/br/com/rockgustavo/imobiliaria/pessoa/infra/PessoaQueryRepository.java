package br.com.rockgustavo.imobiliaria.pessoa.infra;

import br.com.rockgustavo.imobiliaria.pessoa.application.PessoaFiltro;
import br.com.rockgustavo.imobiliaria.pessoa.domain.ClassificacaoComercial;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public class PessoaQueryRepository {

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

    public PessoaQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Page<PessoaResumoView> listar(UUID tenantId, PessoaFiltro filtro, LocalDate hoje, Pageable pageable) {
        String condicoesBase = condicoesBaseDoFiltro(filtro);
        String condicaoClassificacao = filtro.classificacao() != null ? " WHERE t.classificacao = :classificacao" : "";
        long total = contar(tenantId, filtro, hoje, condicoesBase, condicaoClassificacao);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT * FROM (
                  SELECT p.id, p.tipo_documento, p.documento, p.nome, p.email, p.ativo,
                         p.criado_em, p.alterado_em,
                         (SELECT array_agg(pp.papel ORDER BY pp.papel)
                            FROM pessoa_papel pp
                           WHERE pp.tenant_id = :tenantId AND pp.pessoa_id = p.id) AS papeis,
                         """ + CASE_CLASSIFICACAO + """
                          AS classificacao
                    FROM pessoa p
                   WHERE p.tenant_id = :tenantId
                  """ + condicoesBase + """
                ) t
                """ + condicaoClassificacao + """
                 ORDER BY t.criado_em DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenantId", tenantId)
                .param("hoje", hoje)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset());
        consulta = aplicarParametrosDeFiltro(consulta, filtro);

        List<PessoaResumoView> conteudo = consulta.query(this::mapear).list();
        return new PageImpl<>(conteudo, pageable, total);
    }

    public ClassificacaoComercial classificacaoDe(UUID tenantId, UUID pessoaId, LocalDate hoje) {
        String classificacao = jdbcClient.sql("""
                SELECT
                """ + CASE_CLASSIFICACAO + """
                  FROM pessoa p
                 WHERE p.tenant_id = :tenantId AND p.id = :pessoaId
                """)
                .param("tenantId", tenantId)
                .param("pessoaId", pessoaId)
                .param("hoje", hoje)
                .query(String.class)
                .single();
        return ClassificacaoComercial.valueOf(classificacao);
    }

    private long contar(UUID tenantId, PessoaFiltro filtro, LocalDate hoje, String condicoesBase, String condicaoClassificacao) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT count(*) FROM (
                  SELECT p.id,
                         """ + CASE_CLASSIFICACAO + """
                          AS classificacao
                    FROM pessoa p
                   WHERE p.tenant_id = :tenantId
                  """ + condicoesBase + """
                ) t
                """ + condicaoClassificacao)
                .param("tenantId", tenantId)
                .param("hoje", hoje);
        consulta = aplicarParametrosDeFiltro(consulta, filtro);
        return consulta.query(Long.class).single();
    }

    private String condicoesBaseDoFiltro(PessoaFiltro filtro) {
        StringBuilder condicoes = new StringBuilder();
        if (filtro.documento() != null) {
            condicoes.append(" AND p.documento = :documento");
        }
        if (filtro.ativo() != null) {
            condicoes.append(" AND p.ativo = :ativo");
        }
        if (filtro.papel() != null) {
            condicoes.append("""
                     AND EXISTS (SELECT 1 FROM pessoa_papel pp2
                                  WHERE pp2.tenant_id = :tenantId AND pp2.pessoa_id = p.id AND pp2.papel = :papel)
                    """);
        }
        return condicoes.toString();
    }

    private JdbcClient.StatementSpec aplicarParametrosDeFiltro(JdbcClient.StatementSpec consulta, PessoaFiltro filtro) {
        if (filtro.documento() != null) {
            consulta = consulta.param("documento", filtro.documento());
        }
        if (filtro.ativo() != null) {
            consulta = consulta.param("ativo", filtro.ativo());
        }
        if (filtro.papel() != null) {
            consulta = consulta.param("papel", filtro.papel().name());
        }
        if (filtro.classificacao() != null) {
            consulta = consulta.param("classificacao", filtro.classificacao().name());
        }
        return consulta;
    }

    private PessoaResumoView mapear(ResultSet rs, int rowNum) throws SQLException {
        Array papeisArray = rs.getArray("papeis");
        List<Papel> papeis = papeisArray == null
                ? List.of()
                : Arrays.stream((String[]) papeisArray.getArray()).map(Papel::valueOf).toList();
        return new PessoaResumoView(
                (UUID) rs.getObject("id"),
                TipoDocumento.valueOf(rs.getString("tipo_documento")),
                rs.getString("documento"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getBoolean("ativo"),
                papeis,
                ClassificacaoComercial.valueOf(rs.getString("classificacao")),
                rs.getTimestamp("criado_em").toInstant(),
                rs.getTimestamp("alterado_em").toInstant());
    }
}
