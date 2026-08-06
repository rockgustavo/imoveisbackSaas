package br.com.rockgustavo.imobiliaria.painel.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.painel.infra.PainelQueryRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class PainelService {

    private final PainelQueryRepository queryRepository;
    private final ImobiliariaFacade imobiliariaFacade;

    public PainelService(PainelQueryRepository queryRepository, ImobiliariaFacade imobiliariaFacade) {
        this.queryRepository = queryRepository;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public PainelIndicadores indicadores() {
        UUID tenantId = TenantContext.obter();
        LocalDate hoje = LocalDate.now(ZoneId.of(imobiliariaFacade.fusoHorario(tenantId)));
        return new PainelIndicadores(
                queryRepository.buscarAgregados(tenantId, hoje),
                queryRepository.buscarImoveisPorSituacao(tenantId),
                queryRepository.buscarFunil(tenantId, hoje));
    }
}
