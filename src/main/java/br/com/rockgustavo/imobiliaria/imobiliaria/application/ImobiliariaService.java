package br.com.rockgustavo.imobiliaria.imobiliaria.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.domain.Imobiliaria;
import br.com.rockgustavo.imobiliaria.imobiliaria.domain.ImobiliariaDuplicadaException;
import br.com.rockgustavo.imobiliaria.imobiliaria.domain.ImobiliariaParametro;
import br.com.rockgustavo.imobiliaria.imobiliaria.infra.ImobiliariaParametroRepository;
import br.com.rockgustavo.imobiliaria.imobiliaria.infra.ImobiliariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImobiliariaService {

    private final ImobiliariaRepository imobiliariaRepository;
    private final ImobiliariaParametroRepository parametroRepository;

    public ImobiliariaService(ImobiliariaRepository imobiliariaRepository,
                               ImobiliariaParametroRepository parametroRepository) {
        this.imobiliariaRepository = imobiliariaRepository;
        this.parametroRepository = parametroRepository;
    }

    @Transactional
    public UUID criar(CriarImobiliariaComando comando) {
        String cnpjLimpo = comando.cnpj() == null ? null : comando.cnpj().replaceAll("\\D", "");
        if (cnpjLimpo != null && imobiliariaRepository.existsByCnpj(cnpjLimpo)) {
            throw ImobiliariaDuplicadaException.porCnpj(comando.cnpj());
        }
        if (imobiliariaRepository.existsBySlug(comando.slug())) {
            throw ImobiliariaDuplicadaException.porSlug(comando.slug());
        }

        Imobiliaria imobiliaria = new Imobiliaria(comando.razaoSocial(), comando.cnpj(), comando.slug());
        imobiliariaRepository.save(imobiliaria);

        ImobiliariaParametro parametro = ImobiliariaParametro.comDefaultsDePlataforma(imobiliaria.getId());
        parametroRepository.save(parametro);

        return imobiliaria.getId();
    }
}
