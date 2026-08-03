package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaParametroService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ParametrosTenant;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ImobiliariaFacade {

    private final ImobiliariaParametroService parametroService;

    public ImobiliariaFacade(ImobiliariaParametroService parametroService) {
        this.parametroService = parametroService;
    }

    public ParametrosTenant parametrosDoTenantAtual() {
        return parametroService.buscarDoTenantAtual();
    }

    public int cepCacheJanelaDias(UUID tenantId) {
        return parametroService.buscarPorTenant(tenantId).cepCacheJanelaDias();
    }

    public short geocodificacaoTentativasMax(UUID tenantId) {
        return parametroService.buscarPorTenant(tenantId).geocodificacaoTentativasMax();
    }
}
