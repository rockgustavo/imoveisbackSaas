package br.com.rockgustavo.imobiliaria.pessoa;

import br.com.rockgustavo.imobiliaria.pessoa.domain.Pessoa;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class PessoaFacade {

    private final PessoaRepository pessoaRepository;

    public PessoaFacade(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public boolean estaAtiva(UUID pessoaId) {
        return pessoaRepository.buscarPorId(pessoaId).map(Pessoa::isAtivo).orElse(false);
    }
}
