package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Endereco;
import br.com.rockgustavo.imobiliaria.propriedade.domain.EnderecoAlterado;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeCadastrada;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeNaoEncontradaException;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeProprietarioInvalidoException;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeQueryRepository;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeResumoView;
import br.com.rockgustavo.imobiliaria.shared.geo.Coordenada;
import br.com.rockgustavo.imobiliaria.shared.geo.EnderecoParaGeocodificar;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PropriedadeService {

    private final PropriedadeRepository propriedadeRepository;
    private final PropriedadeQueryRepository queryRepository;
    private final PessoaFacade pessoaFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final GeocodificacaoService geocodificacaoService;

    public PropriedadeService(PropriedadeRepository propriedadeRepository, PropriedadeQueryRepository queryRepository,
                               PessoaFacade pessoaFacade, ApplicationEventPublisher eventPublisher,
                               GeocodificacaoService geocodificacaoService) {
        this.propriedadeRepository = propriedadeRepository;
        this.queryRepository = queryRepository;
        this.pessoaFacade = pessoaFacade;
        this.eventPublisher = eventPublisher;
        this.geocodificacaoService = geocodificacaoService;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public UUID criar(CriarPropriedadeComando comando) {
        garantirProprietarioValido(comando.proprietarioId());
        Propriedade propriedade = new Propriedade(comando.proprietarioId(), comando.tipo(), comando.areaPrivativa(),
                comando.quartos(), comando.vagas(), comando.valorReferencia(), paraEndereco(comando));
        propriedadeRepository.save(propriedade);
        eventPublisher.publishEvent(new PropriedadeCadastrada(propriedade.getId(), TenantContext.obter()));
        return propriedade.getId();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public PropriedadeDetalhe buscarPorId(UUID id) {
        return paraDetalhe(buscarEntidade(id));
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public PropriedadeDetalhe atualizar(AtualizarPropriedadeComando comando) {
        Propriedade propriedade = buscarEntidade(comando.propriedadeId());
        if (!comando.proprietarioId().equals(propriedade.getProprietarioId())) {
            garantirProprietarioValido(comando.proprietarioId());
            propriedade.trocarProprietario(comando.proprietarioId());
        }
        Endereco endereco = new Endereco(comando.cep(), comando.logradouro(), comando.numero(), comando.complemento(),
                comando.bairro(), comando.localidade(), comando.uf(), comando.enderecoValidado());
        boolean enderecoMudou = propriedade.atualizar(comando.tipo(), comando.areaPrivativa(), comando.quartos(),
                comando.vagas(), comando.valorReferencia(), endereco);
        if (comando.latitude() != null && comando.longitude() != null) {
            propriedade.concluirGeolocalizacao(comando.latitude(), comando.longitude());
        } else if (enderecoMudou) {
            eventPublisher.publishEvent(new EnderecoAlterado(propriedade.getId(), TenantContext.obter()));
        }
        return paraDetalhe(propriedade);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Optional<Coordenada> pesquisarGeolocalizacao(EnderecoParaGeocodificar endereco) {
        return geocodificacaoService.pesquisar(endereco);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public PropriedadeDetalhe retirar(UUID id) {
        Propriedade propriedade = buscarEntidade(id);
        propriedade.retirar();
        return paraDetalhe(propriedade);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public PropriedadeDetalhe reservar(UUID id) {
        Propriedade propriedade = buscarEntidade(id);
        propriedade.reservar();
        return paraDetalhe(propriedade);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public PropriedadeDetalhe desfazerReserva(UUID id) {
        Propriedade propriedade = buscarEntidade(id);
        propriedade.desfazerReserva();
        return paraDetalhe(propriedade);
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional
    public PropriedadeDetalhe vender(UUID id) {
        Propriedade propriedade = buscarEntidade(id);
        propriedade.vender();
        return paraDetalhe(propriedade);
    }

    @Transactional
    public void marcarAgenciada(UUID id) {
        buscarEntidade(id).agenciar();
    }

    @Transactional
    public void liberarAgenciamento(UUID id) {
        buscarEntidade(id).liberarAgenciamento();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<PropriedadeResumoView> listar(PropriedadeFiltro filtro, Pageable pageable) {
        return queryRepository.listar(TenantContext.obter(), filtro, pageable);
    }

    private void garantirProprietarioValido(UUID pessoaId) {
        if (!pessoaFacade.podeSerProprietario(pessoaId)) {
            throw new PropriedadeProprietarioInvalidoException(pessoaId);
        }
    }

    private Propriedade buscarEntidade(UUID id) {
        return propriedadeRepository.buscarPorId(id).orElseThrow(() -> new PropriedadeNaoEncontradaException(id));
    }

    private static Endereco paraEndereco(CriarPropriedadeComando comando) {
        return new Endereco(comando.cep(), comando.logradouro(), comando.numero(), comando.complemento(),
                comando.bairro(), comando.localidade(), comando.uf(), comando.enderecoValidado());
    }

    private static PropriedadeDetalhe paraDetalhe(Propriedade propriedade) {
        Endereco endereco = propriedade.getEndereco();
        return new PropriedadeDetalhe(
                propriedade.getId(), propriedade.getProprietarioId(), propriedade.getTipo(),
                propriedade.getAreaPrivativa(), propriedade.getQuartos(), propriedade.getVagas(),
                propriedade.getValorReferencia(), propriedade.getSituacao(),
                endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(), endereco.getComplemento(),
                endereco.getBairro(), endereco.getLocalidade(), endereco.getUf(), endereco.isValidado(),
                propriedade.getLatitude(), propriedade.getLongitude(), propriedade.getGeoSituacao(),
                propriedade.getCriadoEm(), propriedade.getAlteradoEm());
    }
}
