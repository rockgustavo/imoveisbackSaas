package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PropriedadeRepository extends JpaRepository<Propriedade, UUID> {

    @Query("select p from Propriedade p where p.id = :id")
    Optional<Propriedade> buscarPorId(@Param("id") UUID id);
}
