package br.com.rockgustavo.imobiliaria.shared.auditoria;

import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class HistoricoTransicaoListener {

    private final HistoricoTransicaoRepository repository;

    public HistoricoTransicaoListener(HistoricoTransicaoRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    public void aoTransicionar(TransicaoStatusOcorrida evento) {
        TenantContext.definir(evento.tenantId());
        try {
            repository.save(new HistoricoTransicao(evento.entidadeTipo(), evento.entidadeId(),
                    evento.statusAnterior(), evento.statusNovo(), evento.autor()));
        } finally {
            TenantContext.limpar();
        }
    }
}
