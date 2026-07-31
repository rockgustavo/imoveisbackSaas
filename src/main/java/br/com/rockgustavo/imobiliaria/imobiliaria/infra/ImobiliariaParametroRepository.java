package br.com.rockgustavo.imobiliaria.imobiliaria.infra;

import br.com.rockgustavo.imobiliaria.imobiliaria.domain.ImobiliariaParametro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImobiliariaParametroRepository extends JpaRepository<ImobiliariaParametro, UUID> {
}
