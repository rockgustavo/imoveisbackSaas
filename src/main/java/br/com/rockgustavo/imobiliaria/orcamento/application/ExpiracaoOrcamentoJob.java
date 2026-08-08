package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoEnviadoView;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoQueryRepository;
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
public class ExpiracaoOrcamentoJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoOrcamentoJob.class);

    private final OrcamentoQueryRepository queryRepository;
    private final OrcamentoService orcamentoService;
    private final ImobiliariaFacade imobiliariaFacade;

    public ExpiracaoOrcamentoJob(OrcamentoQueryRepository queryRepository, OrcamentoService orcamentoService,
                                  ImobiliariaFacade imobiliariaFacade) {
        this.queryRepository = queryRepository;
        this.orcamentoService = orcamentoService;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void executar() {
        List<OrcamentoEnviadoView> candidatos = queryRepository.buscarEnviadosDeTodosOsTenants();
        Map<UUID, String> fusoPorTenant = new HashMap<>();
        int expirados = 0;
        int falhas = 0;
        for (OrcamentoEnviadoView candidato : candidatos) {
            String fuso = fusoPorTenant.computeIfAbsent(candidato.tenantId(), imobiliariaFacade::fusoHorario);
            LocalDate hoje = LocalDate.now(ZoneId.of(fuso));
            if (!candidato.validade().isBefore(hoje)) {
                continue;
            }
            TenantContext.definir(candidato.tenantId());
            try {
                orcamentoService.expirarSeAplicavel(candidato.id(), hoje);
                expirados++;
            } catch (RuntimeException e) {
                falhas++;
                log.error("RN-05-03: falha ao expirar orçamento {} do tenant {}; a rotina segue nos demais",
                        candidato.id(), candidato.tenantId(), e);
            } finally {
                TenantContext.limpar();
            }
        }
        log.info("RN-05-03: rotina de expiração de orçamento expirou {} de {} orçamentos ENVIADO, {} com falha",
                expirados, candidatos.size(), falhas);
    }
}
