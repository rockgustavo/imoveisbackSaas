package br.com.rockgustavo.imobiliaria.shared.geo;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
public class CepCacheRepository {

    private final JdbcClient jdbcClient;

    public CepCacheRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<CepConsulta> buscarValido(String cep, int janelaDias) {
        Timestamp limite = Timestamp.from(Instant.now().minus(Duration.ofDays(janelaDias)));
        return jdbcClient.sql("""
                SELECT cep, logradouro, bairro, localidade, uf, latitude, longitude, encontrado
                  FROM cep_cache
                 WHERE cep = :cep
                   AND consultado_em >= :limite
                """)
                .param("cep", cep)
                .param("limite", limite)
                .query(this::mapear)
                .optional();
    }

    public void salvar(CepConsulta consulta) {
        jdbcClient.sql("""
                INSERT INTO cep_cache (cep, logradouro, bairro, localidade, uf, latitude, longitude, encontrado, consultado_em)
                VALUES (:cep, :logradouro, :bairro, :localidade, :uf, :latitude, :longitude, :encontrado, now())
                ON CONFLICT (cep) DO UPDATE SET
                    logradouro = EXCLUDED.logradouro,
                    bairro = EXCLUDED.bairro,
                    localidade = EXCLUDED.localidade,
                    uf = EXCLUDED.uf,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    encontrado = EXCLUDED.encontrado,
                    consultado_em = now()
                """)
                .param("cep", consulta.cep())
                .param("logradouro", consulta.logradouro())
                .param("bairro", consulta.bairro())
                .param("localidade", consulta.localidade())
                .param("uf", consulta.uf())
                .param("latitude", consulta.latitude())
                .param("longitude", consulta.longitude())
                .param("encontrado", consulta.encontrado())
                .update();
    }

    private CepConsulta mapear(ResultSet rs, int rowNum) throws SQLException {
        return new CepConsulta(
                rs.getString("cep").trim(),
                rs.getBoolean("encontrado"),
                rs.getString("logradouro"),
                rs.getString("bairro"),
                rs.getString("localidade"),
                rs.getString("uf"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"));
    }
}
