package br.com.rockgustavo.imobiliaria.imobiliaria;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaParametroService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ParametrosTenant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ImobiliariaFacade {

    private final ImobiliariaParametroService parametroService;
    private final ImobiliariaService imobiliariaService;

    public ImobiliariaFacade(ImobiliariaParametroService parametroService, ImobiliariaService imobiliariaService) {
        this.parametroService = parametroService;
        this.imobiliariaService = imobiliariaService;
    }

    public Identificacao identificacao(UUID tenantId) {
        return imobiliariaService.buscarIdentificacao(tenantId);
    }

    public record Identificacao(String razaoSocial, String cnpj) {
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

    public int orcamentoValidadeDiasPadrao(UUID tenantId) {
        return parametroService.buscarPorTenant(tenantId).orcamentoValidadeDiasPadrao();
    }

    public String fusoHorario(UUID tenantId) {
        return parametroService.buscarPorTenant(tenantId).fusoHorario();
    }

    public BigDecimal comissaoPercentualTeto(UUID tenantId) {
        return parametroService.buscarPorTenant(tenantId).comissaoPercentualTeto();
    }
}
