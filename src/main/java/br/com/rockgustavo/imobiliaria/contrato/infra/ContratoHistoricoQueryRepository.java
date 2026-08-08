package br.com.rockgustavo.imobiliaria.contrato.infra;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContratoHistoricoQueryRepository {

    private final JdbcClient jdbcClient;

    public ContratoHistoricoQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<ContratoHistoricoView> buscarEstadoEm(UUID tenantId, UUID contratoId, Instant antesDe) {
        return jdbcClient.sql("""
                SELECT versao, snapshot::text AS snapshot, ocorrido_em
                  FROM contrato_historico
                 WHERE tenant_id = :tenantId
                   AND contrato_id = :contratoId
                   AND ocorrido_em < :antesDe
                 ORDER BY ocorrido_em DESC, versao DESC
                 LIMIT 1
                """)
                .param("tenantId", tenantId)
                .param("contratoId", contratoId)
                .param("antesDe", Timestamp.from(antesDe))
                .query(this::mapear)
                .optional();
    }

    private ContratoHistoricoView mapear(ResultSet rs, int rowNum) throws SQLException {
        return new ContratoHistoricoView(
                rs.getInt("versao"),
                rs.getString("snapshot"),
                rs.getTimestamp("ocorrido_em").toInstant());
    }
}
