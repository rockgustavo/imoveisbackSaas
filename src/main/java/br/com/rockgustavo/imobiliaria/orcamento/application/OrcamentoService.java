package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.domain.Orcamento;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoComissaoAcimaDoTetoException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoNaoEncontradoException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoPropriedadeIndisponivelException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.PessoaInativaNaoRecebeOrcamentoException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoQueryRepository;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoRepository;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoResumoView;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoQueryRepository queryRepository;
    private final PessoaFacade pessoaFacade;
    private final PropriedadeFacade propriedadeFacade;
    private final ImobiliariaFacade imobiliariaFacade;

    public OrcamentoService(OrcamentoRepository orcamentoRepository, OrcamentoQueryRepository queryRepository,
                             PessoaFacade pessoaFacade, PropriedadeFacade propriedadeFacade,
                             ImobiliariaFacade imobiliariaFacade) {
        this.orcamentoRepository = orcamentoRepository;
        this.queryRepository = queryRepository;
        this.pessoaFacade = pessoaFacade;
        this.propriedadeFacade = propriedadeFacade;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public UUID criar(CriarOrcamentoComando comando) {
        garantirPessoaAtiva(comando.pessoaId());
        garantirPropriedadesExistem(comando.itens());
        LocalDate validade = calcularValidade(TenantContext.obter());
        Orcamento orcamento = new Orcamento(comando.pessoaId(), validade, paraItensPropostos(comando.itens()));
        orcamentoRepository.save(orcamento);
        return orcamento.getId();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public OrcamentoDetalhe buscarPorId(UUID id) {
        return paraDetalhe(buscarEntidade(id));
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public OrcamentoDetalhe atualizar(AtualizarOrcamentoComando comando) {
        Orcamento orcamento = buscarEntidade(comando.orcamentoId());
        garantirPropriedadesExistem(comando.itens());
        orcamento.atualizar(paraItensPropostos(comando.itens()));
        return paraDetalhe(orcamento);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public OrcamentoDetalhe enviar(UUID id) {
        Orcamento orcamento = buscarEntidade(id);
        UUID tenantId = TenantContext.obter();
        BigDecimal teto = imobiliariaFacade.comissaoPercentualTeto(tenantId);
        for (var item : orcamento.getItens()) {
            if (!propriedadeFacade.disponivelParaAgenciamentoPor(item.getPropriedadeId(), orcamento.getPessoaId())) {
                throw new OrcamentoPropriedadeIndisponivelException(item.getPropriedadeId());
            }
            if (item.getComissaoPercentual().compareTo(teto) > 0) {
                throw new OrcamentoComissaoAcimaDoTetoException(item.getComissaoPercentual(), teto);
            }
        }
        orcamento.enviar();
        return paraDetalhe(orcamento);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public OrcamentoDetalhe aceitar(UUID id) {
        Orcamento orcamento = buscarEntidade(id);
        orcamento.aceitar(hojeNoFusoDoTenant());
        return paraDetalhe(orcamento);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public OrcamentoDetalhe recusar(UUID id) {
        Orcamento orcamento = buscarEntidade(id);
        orcamento.recusar(hojeNoFusoDoTenant());
        return paraDetalhe(orcamento);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public UUID duplicar(UUID id) {
        Orcamento original = buscarEntidade(id);
        UUID raiz = original.getOrigemId() != null ? original.getOrigemId() : original.getId();
        int novaVersao = orcamentoRepository.maiorVersaoPorOrigem(raiz) + 1;
        LocalDate novaValidade = calcularValidade(TenantContext.obter());
        Orcamento copia = original.duplicar(novaValidade, novaVersao, raiz);
        orcamentoRepository.save(copia);
        return copia.getId();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<OrcamentoResumoView> listar(OrcamentoFiltro filtro, Pageable pageable) {
        return queryRepository.listar(TenantContext.obter(), filtro, pageable);
    }

    @Transactional
    public void expirarSeAplicavel(UUID id, LocalDate hojeNoFusoDoTenant) {
        orcamentoRepository.buscarPorId(id).ifPresent(orcamento -> {
            if (orcamento.getStatus() == StatusOrcamento.ENVIADO && orcamento.getValidade().isBefore(hojeNoFusoDoTenant)) {
                orcamento.expirar();
            }
        });
    }

    private void garantirPessoaAtiva(UUID pessoaId) {
        if (!pessoaFacade.estaAtiva(pessoaId)) {
            throw new PessoaInativaNaoRecebeOrcamentoException(pessoaId);
        }
    }

    private void garantirPropriedadesExistem(List<ItemComando> itens) {
        for (ItemComando item : itens) {
            if (!propriedadeFacade.existe(item.propriedadeId())) {
                throw new OrcamentoPropriedadeIndisponivelException(item.propriedadeId());
            }
        }
    }

    private LocalDate calcularValidade(UUID tenantId) {
        LocalDate hoje = LocalDate.now(ZoneId.of(imobiliariaFacade.fusoHorario(tenantId)));
        return hoje.plusDays(imobiliariaFacade.orcamentoValidadeDiasPadrao(tenantId));
    }

    private LocalDate hojeNoFusoDoTenant() {
        return LocalDate.now(ZoneId.of(imobiliariaFacade.fusoHorario(TenantContext.obter())));
    }

    private Orcamento buscarEntidade(UUID id) {
        return orcamentoRepository.buscarPorId(id).orElseThrow(() -> new OrcamentoNaoEncontradoException(id));
    }

    private static List<Orcamento.ItemProposto> paraItensPropostos(List<ItemComando> itens) {
        return itens.stream()
                .map(item -> new Orcamento.ItemProposto(item.propriedadeId(), item.comissaoPercentual(), item.valorPedido()))
                .toList();
    }

    private static OrcamentoDetalhe paraDetalhe(Orcamento orcamento) {
        List<OrcamentoItemDetalhe> itens = orcamento.getItens().stream()
                .map(item -> new OrcamentoItemDetalhe(item.getPropriedadeId(), item.getComissaoPercentual(), item.getValorPedido()))
                .toList();
        return new OrcamentoDetalhe(
                orcamento.getId(), orcamento.getPessoaId(), orcamento.getStatus(), orcamento.getValidade(),
                orcamento.getOrigemId(), orcamento.getVersao(), itens,
                orcamento.getCriadoEm(), orcamento.getAlteradoEm());
    }
}
