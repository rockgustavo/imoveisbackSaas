package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.EnderecoAlterado;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeCadastrada;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GeocodificacaoListener {

    private final GeocodificacaoService geocodificacaoService;

    public GeocodificacaoListener(GeocodificacaoService geocodificacaoService) {
        this.geocodificacaoService = geocodificacaoService;
    }

    @ApplicationModuleListener
    public void aoCadastrar(PropriedadeCadastrada evento) {
        processar(evento.propriedadeId(), evento.tenantId());
    }

    @ApplicationModuleListener
    public void aoAlterarEndereco(EnderecoAlterado evento) {
        processar(evento.propriedadeId(), evento.tenantId());
    }

    private void processar(UUID propriedadeId, UUID tenantId) {
        TenantContext.definir(tenantId);
        try {
            geocodificacaoService.tentarGeocodificar(propriedadeId);
        } finally {
            TenantContext.limpar();
        }
    }
}
