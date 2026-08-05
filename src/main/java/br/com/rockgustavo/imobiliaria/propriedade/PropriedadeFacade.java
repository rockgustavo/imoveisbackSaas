package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeService;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PropriedadeFacade {

    private final PropriedadeRepository propriedadeRepository;
    private final PropriedadeService propriedadeService;

    public PropriedadeFacade(PropriedadeRepository propriedadeRepository, PropriedadeService propriedadeService) {
        this.propriedadeRepository = propriedadeRepository;
        this.propriedadeService = propriedadeService;
    }

    public boolean existe(UUID propriedadeId) {
        return propriedadeRepository.existsById(propriedadeId);
    }

    public boolean disponivelParaAgenciamentoPor(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioIdAndSituacao(
                propriedadeId, proprietarioId, SituacaoPropriedade.DISPONIVEL);
    }

    public boolean pertenceAoProprietario(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioId(propriedadeId, proprietarioId);
    }

    public boolean semNegociacaoEmAndamento(UUID propriedadeId) {
        return propriedadeRepository.existsByIdAndSituacaoNotIn(
                propriedadeId, List.of(SituacaoPropriedade.RESERVADA, SituacaoPropriedade.VENDIDA));
    }

    public boolean podeSerAgenciadaPor(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioIdAndSituacaoNotIn(propriedadeId, proprietarioId,
                List.of(SituacaoPropriedade.RESERVADA, SituacaoPropriedade.VENDIDA, SituacaoPropriedade.RETIRADA));
    }

    public void agenciar(UUID propriedadeId) {
        propriedadeService.marcarAgenciada(propriedadeId);
    }

    public void liberarAgenciamento(UUID propriedadeId) {
        propriedadeService.liberarAgenciamento(propriedadeId);
    }
}
