package br.com.rockgustavo.imobiliaria.propriedade.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliacaoSituacaoJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacaoSituacaoJob.class);

    @Scheduled(cron = "0 0 3 * * *")
    public void executar() {
        log.info("RN-03-09: reconciliação de situação de propriedade não executada — módulo contrato "
                + "(Épico 06) ainda não existe, nada para comparar contra AGENCIADA");
    }
}
