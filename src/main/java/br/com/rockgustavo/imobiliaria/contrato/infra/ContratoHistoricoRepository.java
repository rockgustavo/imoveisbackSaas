package br.com.rockgustavo.imobiliaria.contrato.infra;

import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoHistorico;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContratoHistoricoRepository extends Repository<ContratoHistorico, UUID> {

    ContratoHistorico save(ContratoHistorico historico);

    ContratoHistorico saveAndFlush(ContratoHistorico historico);

    @Query("select coalesce(max(h.versao), 0) from ContratoHistorico h where h.contratoId = :contratoId")
    int maiorVersaoPorContrato(@Param("contratoId") UUID contratoId);
}
