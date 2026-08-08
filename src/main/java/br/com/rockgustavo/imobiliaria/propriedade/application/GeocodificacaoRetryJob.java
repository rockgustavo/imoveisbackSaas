package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadePendenteGeoView;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeQueryRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GeocodificacaoRetryJob {

    private static final Logger log = LoggerFactory.getLogger(GeocodificacaoRetryJob.class);
    private static final Duration BACKOFF_BASE = Duration.ofMinutes(5);

    private final PropriedadeQueryRepository queryRepository;
    private final GeocodificacaoService geocodificacaoService;
    private final ImobiliariaFacade imobiliariaFacade;

    public GeocodificacaoRetryJob(PropriedadeQueryRepository queryRepository, GeocodificacaoService geocodificacaoService,
                                   ImobiliariaFacade imobiliariaFacade) {
        this.queryRepository = queryRepository;
        this.geocodificacaoService = geocodificacaoService;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void executar() {
        List<PropriedadePendenteGeoView> candidatas = queryRepository.buscarPendentesDeGeoDeTodosOsTenants();
        Map<UUID, Short> tentativasMaxPorTenant = new HashMap<>();
        Instant agora = Instant.now();
        int reprocessadas = 0;
        int falhas = 0;
        for (PropriedadePendenteGeoView candidata : candidatas) {
            short tentativasMax = tentativasMaxPorTenant.computeIfAbsent(candidata.tenantId(),
                    imobiliariaFacade::geocodificacaoTentativasMax);
            if (!elegivelParaRetry(candidata, tentativasMax, agora)) {
                continue;
            }
            TenantContext.definir(candidata.tenantId());
            try {
                geocodificacaoService.tentarGeocodificar(candidata.id());
                reprocessadas++;
            } catch (RuntimeException e) {
                falhas++;
                log.error("RN-04-05: falha ao reprocessar geolocalização da propriedade {} do tenant {}; "
                        + "a rotina segue nas demais", candidata.id(), candidata.tenantId(), e);
            } finally {
                TenantContext.limpar();
            }
        }
        log.info("RN-04-05: retry de geolocalização reprocessou {} de {} propriedades pendentes, {} com falha",
                reprocessadas, candidatas.size(), falhas);
    }

    private boolean elegivelParaRetry(PropriedadePendenteGeoView candidata, short tentativasMax, Instant agora) {
        if (candidata.geoTentativas() >= tentativasMax) {
            return false;
        }
        Duration backoff = BACKOFF_BASE.multipliedBy(1L << candidata.geoTentativas());
        return !candidata.alteradoEm().plus(backoff).isAfter(agora);
    }
}
