package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeAgenciadaView;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReconciliacaoSituacaoJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacaoSituacaoJob.class);

    private final PropriedadeQueryRepository queryRepository;

    public ReconciliacaoSituacaoJob(PropriedadeQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void executar() {
        List<PropriedadeAgenciadaView> agenciadas = queryRepository.propriedadesAgenciadasDeTodosOsTenants();
        Set<UUID> comAgenciamentoVigente = queryRepository.propriedadesComAgenciamentoVigenteDeTodosOsTenants();

        int agenciadaSemAgenciamento = 0;
        for (PropriedadeAgenciadaView agenciada : agenciadas) {
            if (!comAgenciamentoVigente.contains(agenciada.id())) {
                log.warn("RN-03-09: propriedade {} (tenant {}) está AGENCIADA sem agenciamento vigente correspondente",
                        agenciada.id(), agenciada.tenantId());
                agenciadaSemAgenciamento++;
            }
        }

        Set<UUID> agenciadaIds = agenciadas.stream().map(PropriedadeAgenciadaView::id).collect(Collectors.toSet());
        int agenciamentoSemSituacao = 0;
        for (UUID propriedadeId : comAgenciamentoVigente) {
            if (!agenciadaIds.contains(propriedadeId)) {
                log.warn("RN-03-09: propriedade {} tem agenciamento vigente mas situação não é AGENCIADA", propriedadeId);
                agenciamentoSemSituacao++;
            }
        }

        log.info("RN-03-09: reconciliação concluída — {} propriedade(s) AGENCIADA sem agenciamento vigente, "
                        + "{} agenciamento(s) vigente(s) sem situação AGENCIADA",
                agenciadaSemAgenciamento, agenciamentoSemSituacao);
    }
}
