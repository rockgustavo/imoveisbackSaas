package br.com.rockgustavo.imobiliaria.contrato;

import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContratoFacade {

    private final ContratoRepository contratoRepository;

    public ContratoFacade(ContratoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    public boolean existe(UUID contratoId) {
        return contratoRepository.existsById(contratoId);
    }
}
