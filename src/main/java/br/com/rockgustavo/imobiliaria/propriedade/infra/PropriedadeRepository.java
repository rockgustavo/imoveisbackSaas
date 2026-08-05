package br.com.rockgustavo.imobiliaria.propriedade.infra;

import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PropriedadeRepository extends JpaRepository<Propriedade, UUID> {

    @Query("select p from Propriedade p where p.id = :id")
    Optional<Propriedade> buscarPorId(@Param("id") UUID id);

    boolean existsByIdAndProprietarioIdAndSituacao(UUID id, UUID proprietarioId, SituacaoPropriedade situacao);

    boolean existsByIdAndProprietarioId(UUID id, UUID proprietarioId);

    boolean existsByIdAndSituacaoNotIn(UUID id, Collection<SituacaoPropriedade> situacoesExcluidas);

    boolean existsByIdAndProprietarioIdAndSituacaoNotIn(UUID id, UUID proprietarioId,
                                                          Collection<SituacaoPropriedade> situacoesExcluidas);
}
