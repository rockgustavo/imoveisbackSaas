package br.com.rockgustavo.imobiliaria.imobiliaria.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.domain.ImobiliariaParametro;
import br.com.rockgustavo.imobiliaria.imobiliaria.domain.TenantSemParametroException;
import br.com.rockgustavo.imobiliaria.imobiliaria.infra.ImobiliariaParametroRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ImobiliariaParametroService {

    private final ImobiliariaParametroRepository repository;

    public ImobiliariaParametroService(ImobiliariaParametroRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ParametrosTenant buscarDoTenantAtual() {
        return paraView(buscarEntidadeDoTenantAtual());
    }

    @Transactional(readOnly = true)
    public ParametrosTenant buscarPorTenant(UUID tenantId) {
        return paraView(repository.findById(tenantId).orElseThrow(() -> new TenantSemParametroException(tenantId)));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ParametrosTenant atualizar(AtualizarParametrosComando comando) {
        ImobiliariaParametro parametro = buscarEntidadeDoTenantAtual();
        parametro.atualizar(
                comando.comissaoPercentualTeto(),
                comando.orcamentoValidadeDiasPadrao(),
                comando.geocodificacaoTentativasMax(),
                comando.cepCacheJanelaDias(),
                comando.fusoHorario());
        return paraView(parametro);
    }

    private ImobiliariaParametro buscarEntidadeDoTenantAtual() {
        UUID tenantId = TenantContext.obter();
        return repository.findById(tenantId).orElseThrow(() -> new TenantSemParametroException(tenantId));
    }

    private ParametrosTenant paraView(ImobiliariaParametro parametro) {
        return new ParametrosTenant(
                parametro.getComissaoPercentualTeto(),
                parametro.getOrcamentoValidadeDiasPadrao(),
                parametro.getGeocodificacaoTentativasMax(),
                parametro.getCepCacheJanelaDias(),
                parametro.getFusoHorario());
    }
}
