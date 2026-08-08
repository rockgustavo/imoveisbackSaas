package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.Agenciamento;
import br.com.rockgustavo.imobiliaria.contrato.domain.Contrato;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoCancelamentoInviavelException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoHistoricoNaoEncontradoException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoNaoEncontradoException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoPropriedadeIndisponivelException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoVigenciaSobrepostaException;
import br.com.rockgustavo.imobiliaria.contrato.domain.OrcamentoJaOriginouContratoException;
import br.com.rockgustavo.imobiliaria.contrato.domain.PropriedadeProprietarioDivergenteException;
import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;
import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;
import br.com.rockgustavo.imobiliaria.contrato.infra.ConflitoVigenciaView;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoQueryRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoView;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoQueryRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoResumoView;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade.ItemAceito;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade.OrcamentoAceitoDetalhe;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.shared.auditoria.EntidadeAuditavel;
import br.com.rockgustavo.imobiliaria.shared.auditoria.TransicaoStatusOcorrida;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContratoService {

    private static final String CONSTRAINT_VIGENCIA_EXCLUSIVA = "agenciamento_vigencia_excl";

    private final ContratoRepository contratoRepository;
    private final ContratoQueryRepository queryRepository;
    private final ContratoHistoricoService contratoHistoricoService;
    private final ContratoHistoricoQueryRepository contratoHistoricoQueryRepository;
    private final OrcamentoFacade orcamentoFacade;
    private final PropriedadeFacade propriedadeFacade;
    private final ImobiliariaFacade imobiliariaFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditorAware<UUID> auditorAware;

    public ContratoService(ContratoRepository contratoRepository, ContratoQueryRepository queryRepository,
                            ContratoHistoricoService contratoHistoricoService,
                            ContratoHistoricoQueryRepository contratoHistoricoQueryRepository,
                            OrcamentoFacade orcamentoFacade, PropriedadeFacade propriedadeFacade,
                            ImobiliariaFacade imobiliariaFacade, ApplicationEventPublisher eventPublisher,
                            AuditorAware<UUID> auditorAware) {
        this.contratoRepository = contratoRepository;
        this.queryRepository = queryRepository;
        this.contratoHistoricoService = contratoHistoricoService;
        this.contratoHistoricoQueryRepository = contratoHistoricoQueryRepository;
        this.orcamentoFacade = orcamentoFacade;
        this.propriedadeFacade = propriedadeFacade;
        this.imobiliariaFacade = imobiliariaFacade;
        this.eventPublisher = eventPublisher;
        this.auditorAware = auditorAware;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public UUID criar(CriarContratoComando comando) {
        if (contratoRepository.existsByOrcamentoOrigemId(comando.orcamentoId())) {
            throw new OrcamentoJaOriginouContratoException(comando.orcamentoId());
        }
        OrcamentoAceitoDetalhe aceito = orcamentoFacade.buscarAceito(comando.orcamentoId());
        List<Contrato.ItemParaAgenciar> itens = new ArrayList<>();
        for (ItemAceito item : aceito.itens()) {
            if (!propriedadeFacade.pertenceAoProprietario(item.propriedadeId(), aceito.pessoaId())) {
                throw new PropriedadeProprietarioDivergenteException(item.propriedadeId(), aceito.pessoaId());
            }
            itens.add(new Contrato.ItemParaAgenciar(item.propriedadeId(), item.comissaoPercentual(), item.valorPedido()));
        }
        Contrato contrato = new Contrato(aceito.pessoaId(), comando.orcamentoId(), comando.vigenciaInicio(),
                comando.vigenciaFim(), comando.regrasContratuais(), itens);
        contratoRepository.save(contrato);
        publicarTransicao(contrato, null);
        return contrato.getId();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ContratoDetalhe buscarPorId(UUID id) {
        return paraDetalhe(buscarEntidade(id));
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public ContratoDetalhe ativar(UUID id) {
        Contrato contrato = buscarEntidade(id);
        StatusContrato anterior = contrato.getStatus();
        UUID tenantId = TenantContext.obter();
        for (Agenciamento agenciamento : contrato.getAgenciamentos()) {
            exigirDisponibilidadeENaoConflito(tenantId, contrato, agenciamento.getPropriedadeId());
        }
        BigDecimal teto = imobiliariaFacade.comissaoPercentualTeto(tenantId);
        try {
            contrato.ativar(teto);
            contratoRepository.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException e) {
            traduzirConflitoDeVigencia(e, contrato);
        } catch (CannotAcquireLockException e) {
            throw perdeuACorridaPelaVigencia(contrato);
        }
        contrato.getAgenciamentos().forEach(a -> propriedadeFacade.agenciar(a.getPropriedadeId()));
        publicarTransicao(contrato, anterior.name());
        registrarHistorico(contrato);
        return paraDetalhe(contrato);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public ContratoDetalhe encerrar(UUID id, String justificativa) {
        Contrato contrato = buscarEntidade(id);
        StatusContrato anterior = contrato.getStatus();
        List<UUID> propriedadeIds = contrato.getPropriedadeIds();
        contrato.encerrar(justificativa);
        propriedadeIds.forEach(propriedadeFacade::liberarAgenciamento);
        publicarTransicao(contrato, anterior.name());
        registrarHistorico(contrato);
        return paraDetalhe(contrato);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public ContratoDetalhe cancelar(UUID id) {
        Contrato contrato = buscarEntidade(id);
        StatusContrato anterior = contrato.getStatus();
        boolean eraAtivo = anterior == StatusContrato.ATIVO;
        if (eraAtivo) {
            for (UUID propriedadeId : contrato.getPropriedadeIds()) {
                if (!propriedadeFacade.semNegociacaoEmAndamento(propriedadeId)) {
                    throw new ContratoCancelamentoInviavelException(id, propriedadeId);
                }
            }
        }
        List<UUID> propriedadeIds = contrato.getPropriedadeIds();
        contrato.cancelar();
        if (eraAtivo) {
            propriedadeIds.forEach(propriedadeFacade::liberarAgenciamento);
        }
        publicarTransicao(contrato, anterior.name());
        registrarHistorico(contrato);
        return paraDetalhe(contrato);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public ContratoDetalhe registrarAditivo(AditivoComando comando) {
        Contrato contrato = buscarEntidade(comando.contratoId());
        UUID tenantId = TenantContext.obter();
        if (comando.tipo() == TipoAditivo.EXCLUSAO) {
            contrato.excluirPropriedade(comando.propriedadeId(), comando.justificativa());
            propriedadeFacade.liberarAgenciamento(comando.propriedadeId());
            registrarHistorico(contrato);
            return paraDetalhe(contrato);
        }

        boolean jaAgenciada = contrato.possuiAgenciamentoAtivoPara(comando.propriedadeId());
        if (!jaAgenciada) {
            exigirDisponibilidadeENaoConflito(tenantId, contrato, comando.propriedadeId());
        }
        BigDecimal teto = imobiliariaFacade.comissaoPercentualTeto(tenantId);
        try {
            contrato.incluirPropriedade(comando.propriedadeId(), comando.comissaoPercentual(), comando.valorPedido(),
                    teto, comando.justificativa());
            contratoRepository.saveAndFlush(contrato);
        } catch (DataIntegrityViolationException e) {
            traduzirConflitoDeVigencia(e, contrato);
        } catch (CannotAcquireLockException e) {
            throw perdeuACorridaPelaVigencia(contrato);
        }
        if (!jaAgenciada) {
            propriedadeFacade.agenciar(comando.propriedadeId());
        }
        registrarHistorico(contrato);
        return paraDetalhe(contrato);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<ContratoResumoView> listar(ContratoFiltro filtro, Pageable pageable) {
        UUID tenantId = TenantContext.obter();
        LocalDate hoje = filtro.vencendoEmDias() != null
                ? LocalDate.now(ZoneId.of(imobiliariaFacade.fusoHorario(tenantId)))
                : null;
        return queryRepository.listar(tenantId, filtro, hoje, pageable);
    }

    @Transactional
    public void expirarSeAplicavel(UUID id, LocalDate hojeNoFusoDoTenant) {
        contratoRepository.buscarPorId(id).ifPresent(contrato -> {
            if (contrato.getStatus() != StatusContrato.ATIVO || !contrato.getVigenciaFim().isBefore(hojeNoFusoDoTenant)) {
                return;
            }
            StatusContrato anterior = contrato.getStatus();
            List<UUID> propriedadeIds = contrato.getPropriedadeIds();
            contrato.expirar();
            propriedadeIds.forEach(propriedadeFacade::liberarAgenciamento);
            publicarTransicao(contrato, anterior.name());
            registrarHistorico(contrato);
        });
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ContratoHistoricoDetalhe buscarHistoricoEm(UUID id, LocalDate data) {
        buscarEntidade(id);
        UUID tenantId = TenantContext.obter();
        ZoneId fuso = ZoneId.of(imobiliariaFacade.fusoHorario(tenantId));
        Instant ateInstante = data.atTime(LocalTime.MAX).atZone(fuso).toInstant();
        ContratoHistoricoView view = contratoHistoricoQueryRepository.buscarEstadoEm(tenantId, id, ateInstante)
                .orElseThrow(() -> new ContratoHistoricoNaoEncontradoException(id, data));
        return new ContratoHistoricoDetalhe(view.versao(), view.ocorridoEm(), contratoHistoricoService.desserializar(view.snapshot()));
    }

    private void exigirDisponibilidadeENaoConflito(UUID tenantId, Contrato contrato, UUID propriedadeId) {
        if (!propriedadeFacade.podeSerAgenciadaPor(propriedadeId, contrato.getPessoaId())) {
            throw new ContratoPropriedadeIndisponivelException(propriedadeId);
        }
        Optional<ConflitoVigenciaView> conflito = queryRepository.buscarConflito(
                tenantId, propriedadeId, contrato.getVigenciaInicio(), contrato.getVigenciaFim());
        if (conflito.isPresent()) {
            ConflitoVigenciaView v = conflito.get();
            throw new ContratoVigenciaSobrepostaException(propriedadeId, v.contratoId(), v.vigenciaInicio(), v.vigenciaFim());
        }
    }

    private void traduzirConflitoDeVigencia(DataIntegrityViolationException e, Contrato contrato) {
        if (!causaAlgumaEhExclusaoDeVigencia(e)) {
            throw e;
        }
        throw perdeuACorridaPelaVigencia(contrato);
    }

    private ContratoVigenciaSobrepostaException perdeuACorridaPelaVigencia(Contrato contrato) {
        return new ContratoVigenciaSobrepostaException(contrato.getId(), contrato.getVigenciaInicio(), contrato.getVigenciaFim());
    }

    private boolean causaAlgumaEhExclusaoDeVigencia(Throwable excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            boolean nomeDaConstraintBate = causa instanceof ConstraintViolationException cve
                    && CONSTRAINT_VIGENCIA_EXCLUSIVA.equals(cve.getConstraintName());
            boolean mensagemMencionaAConstraint = causa.getMessage() != null
                    && causa.getMessage().contains(CONSTRAINT_VIGENCIA_EXCLUSIVA);
            if (nomeDaConstraintBate || mensagemMencionaAConstraint) {
                return true;
            }
        }
        return false;
    }

    private Contrato buscarEntidade(UUID id) {
        return contratoRepository.buscarPorId(id).orElseThrow(() -> new ContratoNaoEncontradoException(id));
    }

    private void publicarTransicao(Contrato contrato, String statusAnterior) {
        eventPublisher.publishEvent(new TransicaoStatusOcorrida(TenantContext.obter(), EntidadeAuditavel.CONTRATO,
                contrato.getId(), statusAnterior, contrato.getStatus().name(), auditorAware.getCurrentAuditor().orElseThrow()));
    }

    private void registrarHistorico(Contrato contrato) {
        contratoHistoricoService.registrar(paraDetalhe(contrato));
    }

    private static ContratoDetalhe paraDetalhe(Contrato contrato) {
        List<AgenciamentoDetalhe> agenciamentos = contrato.getAgenciamentos().stream()
                .map(a -> new AgenciamentoDetalhe(a.getId(), a.getPropriedadeId(), a.getComissaoPercentual(),
                        a.getValorPedido(), a.isContratoAtivo()))
                .toList();
        List<AditivoDetalhe> aditivos = contrato.getAditivos().stream()
                .map(ad -> new AditivoDetalhe(ad.getPropriedadeId(), ad.getTipo(), ad.getJustificativa(), ad.getData()))
                .toList();
        return new ContratoDetalhe(
                contrato.getId(), contrato.getPessoaId(), contrato.getOrcamentoOrigemId(), contrato.getStatus(),
                contrato.getVigenciaInicio(), contrato.getVigenciaFim(), contrato.getRegrasContratuais(),
                contrato.getJustificativaEncerramento(), agenciamentos, aditivos,
                contrato.getCriadoEm(), contrato.getAlteradoEm());
    }
}
