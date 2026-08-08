package br.com.rockgustavo.imobiliaria.shared.auditoria;

import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface HistoricoTransicaoRepository extends Repository<HistoricoTransicao, UUID> {

    HistoricoTransicao save(HistoricoTransicao historico);
}
