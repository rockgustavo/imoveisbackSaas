package br.com.rockgustavo.imobiliaria.imobiliaria.infra;

import br.com.rockgustavo.imobiliaria.imobiliaria.domain.Imobiliaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImobiliariaRepository extends JpaRepository<Imobiliaria, UUID> {

    boolean existsByCnpj(String cnpj);

    boolean existsBySlug(String slug);
}
