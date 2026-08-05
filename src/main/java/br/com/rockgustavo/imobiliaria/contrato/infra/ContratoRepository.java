package br.com.rockgustavo.imobiliaria.contrato.infra;

import br.com.rockgustavo.imobiliaria.contrato.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContratoRepository extends JpaRepository<Contrato, UUID> {

    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> buscarPorId(@Param("id") UUID id);

    boolean existsByOrcamentoOrigemId(UUID orcamentoOrigemId);
}
