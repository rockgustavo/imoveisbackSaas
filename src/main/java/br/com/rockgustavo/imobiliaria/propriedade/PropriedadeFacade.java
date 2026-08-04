package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PropriedadeFacade {

    private final PropriedadeRepository propriedadeRepository;

    public PropriedadeFacade(PropriedadeRepository propriedadeRepository) {
        this.propriedadeRepository = propriedadeRepository;
    }

    public boolean existe(UUID propriedadeId) {
        return propriedadeRepository.existsById(propriedadeId);
    }

    public boolean disponivelParaAgenciamentoPor(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioIdAndSituacao(
                propriedadeId, proprietarioId, SituacaoPropriedade.DISPONIVEL);
    }
}
