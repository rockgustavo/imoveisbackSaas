package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoAtivoView;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoQueryRepository;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ContratoExpiracaoJob {

    private static final Logger log = LoggerFactory.getLogger(ContratoExpiracaoJob.class);

    private final ContratoQueryRepository queryRepository;
    private final ContratoService contratoService;
    private final ImobiliariaFacade imobiliariaFacade;

    public ContratoExpiracaoJob(ContratoQueryRepository queryRepository, ContratoService contratoService,
                                 ImobiliariaFacade imobiliariaFacade) {
        this.queryRepository = queryRepository;
        this.contratoService = contratoService;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void executar() {
        List<ContratoAtivoView> candidatos = queryRepository.buscarAtivosDeTodosOsTenants();
        Map<UUID, String> fusoPorTenant = new HashMap<>();
        int expirados = 0;
        int falhas = 0;
        for (ContratoAtivoView candidato : candidatos) {
            String fuso = fusoPorTenant.computeIfAbsent(candidato.tenantId(), imobiliariaFacade::fusoHorario);
            LocalDate hoje = LocalDate.now(ZoneId.of(fuso));
            if (!candidato.vigenciaFim().isBefore(hoje)) {
                continue;
            }
            TenantContext.definir(candidato.tenantId());
            try {
                contratoService.expirarSeAplicavel(candidato.id(), hoje);
                expirados++;
            } catch (RuntimeException e) {
                falhas++;
                log.error("RN-06-09: falha ao expirar contrato {} do tenant {}; a rotina segue nos demais",
                        candidato.id(), candidato.tenantId(), e);
            } finally {
                TenantContext.limpar();
            }
        }
        log.info("RN-06-09: rotina de expiração de contrato expirou {} de {} contratos ATIVO, {} com falha",
                expirados, candidatos.size(), falhas);
    }
}
