package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.shared.geo.CepCacheRepository;
import br.com.rockgustavo.imobiliaria.shared.geo.CepClient;
import br.com.rockgustavo.imobiliaria.shared.geo.CepConsulta;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class CepService {

    private final CepClient cepClient;
    private final CepCacheRepository cacheRepository;
    private final ImobiliariaFacade imobiliariaFacade;

    public CepService(CepClient cepClient, CepCacheRepository cacheRepository, ImobiliariaFacade imobiliariaFacade) {
        this.cepClient = cepClient;
        this.cacheRepository = cacheRepository;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    public CepConsulta consultar(String cepBruto) {
        String cep = normalizar(cepBruto);
        if (cep == null) {
            return CepConsulta.naoEncontrado(cepBruto);
        }
        int janelaDias = imobiliariaFacade.cepCacheJanelaDias(TenantContext.obter());
        return cacheRepository.buscarValido(cep, janelaDias).orElseGet(() -> consultarEArmazenar(cep));
    }

    private CepConsulta consultarEArmazenar(String cep) {
        CepConsulta consulta = cepClient.consultar(cep);
        cacheRepository.salvar(consulta);
        return consulta;
    }

    private static String normalizar(String cepBruto) {
        String digitos = cepBruto == null ? "" : cepBruto.replaceAll("\\D", "");
        return digitos.length() == 8 ? digitos : null;
    }
}
