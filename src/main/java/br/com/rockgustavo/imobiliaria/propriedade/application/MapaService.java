package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.MapaPropriedadeView;
import br.com.rockgustavo.imobiliaria.propriedade.infra.MapaQueryRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MapaService {

    static final int TETO_RESULTADOS = 500;

    private final MapaQueryRepository queryRepository;

    public MapaService(MapaQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public MapaResultado buscar(MapaFiltro filtroBruto) {
        MapaFiltro filtro = filtroBruto.comSituacaoDefaultSeAusente(SituacaoPropriedade.AGENCIADA);
        List<MapaPropriedadeView> encontradas = queryRepository.buscar(TenantContext.obter(), filtro, TETO_RESULTADOS + 1);
        boolean limitado = encontradas.size() > TETO_RESULTADOS;
        List<MapaPropriedadeView> propriedades = limitado ? encontradas.subList(0, TETO_RESULTADOS) : encontradas;
        return new MapaResultado(propriedades, limitado);
    }
}
