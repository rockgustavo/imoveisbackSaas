package br.com.rockgustavo.imobiliaria.pessoa.infra;

import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PessoaPapel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaPapelRepository extends JpaRepository<PessoaPapel, UUID> {

    Optional<PessoaPapel> findByPessoaIdAndPapel(UUID pessoaId, Papel papel);

    List<PessoaPapel> findByPessoaId(UUID pessoaId);

    boolean existsByPessoaIdAndPapel(UUID pessoaId, Papel papel);

    @Query("""
            select count(distinct pp.pessoa.id)
              from PessoaPapel pp
             where pp.papel = br.com.rockgustavo.imobiliaria.pessoa.domain.Papel.ADMINISTRADOR
               and pp.pessoa.ativo = true
            """)
    long contarAdministradoresAtivos();
}
