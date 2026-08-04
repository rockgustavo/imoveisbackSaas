package br.com.rockgustavo.imobiliaria.orcamento.infra;

import br.com.rockgustavo.imobiliaria.orcamento.domain.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {

    @Query("select o from Orcamento o where o.id = :id")
    Optional<Orcamento> buscarPorId(@Param("id") UUID id);

    @Query("select coalesce(max(o.versao), 0) from Orcamento o where o.origemId = :raiz or o.id = :raiz")
    int maiorVersaoPorOrigem(@Param("raiz") UUID raiz);
}
