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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
public class PessoaQueryRepository {

    private final JdbcClient jdbcClient;

    public PessoaQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Page<PessoaResumoView> listar(UUID tenantId, PessoaFiltro filtro, Pageable pageable) {
        String condicoes = condicoesDoFiltro(filtro);
        long total = contar(tenantId, filtro, condicoes);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT p.id, p.tipo_documento, p.documento, p.nome, p.email, p.ativo,
                       p.criado_em, p.alterado_em,
                       (SELECT array_agg(pp.papel ORDER BY pp.papel)
                          FROM pessoa_papel pp
                         WHERE pp.tenant_id = :tenantId AND pp.pessoa_id = p.id) AS papeis
                  FROM pessoa p
                 WHERE p.tenant_id = :tenantId
                """ + condicoes + """
                 ORDER BY p.criado_em DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("tenantId", tenantId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset());
        consulta = aplicarParametrosDeFiltro(consulta, filtro);

        List<PessoaResumoView> conteudo = consulta.query(this::mapear).list();
        return new PageImpl<>(conteudo, pageable, total);
    }

    private long contar(UUID tenantId, PessoaFiltro filtro, String condicoes) {
        JdbcClient.StatementSpec consulta = jdbcClient.sql("""
                SELECT count(*)
                  FROM pessoa p
                 WHERE p.tenant_id = :tenantId
                """ + condicoes)
                .param("tenantId", tenantId);
        consulta = aplicarParametrosDeFiltro(consulta, filtro);
        return consulta.query(Long.class).single();
    }

    private String condicoesDoFiltro(PessoaFiltro filtro) {
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
                ClassificacaoComercial.LEAD,
                rs.getTimestamp("criado_em").toInstant(),
                rs.getTimestamp("alterado_em").toInstant());
    }
}
