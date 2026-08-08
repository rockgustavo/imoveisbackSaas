package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoHistorico;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
public class ContratoHistoricoService {

    private static final String CONSTRAINT_VERSAO = "uq_contrato_historico_versao";
    private static final int TENTATIVAS_MAX = 5;

    private final ContratoHistoricoRepository repository;
    private final AuditorAware<UUID> auditorAware;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate novaTransacao;

    public ContratoHistoricoService(ContratoHistoricoRepository repository, AuditorAware<UUID> auditorAware,
                                     ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.auditorAware = auditorAware;
        this.objectMapper = objectMapper;
        this.novaTransacao = new TransactionTemplate(transactionManager);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void registrar(ContratoDetalhe detalhe) {
        String snapshot = serializar(detalhe);
        UUID autor = auditorAware.getCurrentAuditor().orElseThrow();
        for (int tentativa = 1; tentativa <= TENTATIVAS_MAX; tentativa++) {
            try {
                novaTransacao.executeWithoutResult(status -> gravar(detalhe.id(), snapshot, autor));
                return;
            } catch (DataIntegrityViolationException e) {
                if (tentativa == TENTATIVAS_MAX || !colideComVersaoExistente(e)) {
                    throw e;
                }
            }
        }
    }

    public ContratoDetalhe desserializar(String snapshot) {
        try {
            return objectMapper.readValue(snapshot, ContratoDetalhe.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar snapshot de contrato", e);
        }
    }

    private void gravar(UUID contratoId, String snapshot, UUID autor) {
        int proximaVersao = repository.maiorVersaoPorContrato(contratoId) + 1;
        repository.saveAndFlush(new ContratoHistorico(contratoId, proximaVersao, snapshot, autor));
    }

    private String serializar(ContratoDetalhe detalhe) {
        try {
            return objectMapper.writeValueAsString(detalhe);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar snapshot de contrato", e);
        }
    }

    private boolean colideComVersaoExistente(Throwable excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            boolean nomeDaConstraintBate = causa instanceof ConstraintViolationException cve
                    && CONSTRAINT_VERSAO.equals(cve.getConstraintName());
            boolean mensagemMencionaAConstraint = causa.getMessage() != null
                    && causa.getMessage().contains(CONSTRAINT_VERSAO);
            if (nomeDaConstraintBate || mensagemMencionaAConstraint) {
                return true;
            }
        }
        return false;
    }
}
