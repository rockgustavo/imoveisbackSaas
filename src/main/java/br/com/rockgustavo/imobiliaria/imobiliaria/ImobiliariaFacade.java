package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaParametroService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ParametrosTenant;
import org.springframework.stereotype.Component;

@Component
public class ImobiliariaFacade {

    private final ImobiliariaParametroService parametroService;

    public ImobiliariaFacade(ImobiliariaParametroService parametroService) {
        this.parametroService = parametroService;
    }

    public ParametrosTenant parametrosDoTenantAtual() {
        return parametroService.buscarDoTenantAtual();
    }
}
