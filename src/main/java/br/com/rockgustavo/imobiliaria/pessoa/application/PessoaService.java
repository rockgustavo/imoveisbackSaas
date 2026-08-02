package br.com.rockgustavo.imobiliaria.pessoa.application;

import br.com.rockgustavo.imobiliaria.pessoa.domain.ClassificacaoComercial;
import br.com.rockgustavo.imobiliaria.pessoa.domain.DocumentoDuplicadoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.EmailDuplicadoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.EmailObrigatorioException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PapelJaAtribuidoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PapelNaoAtribuidoException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Pessoa;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PessoaNaoEncontradaException;
import br.com.rockgustavo.imobiliaria.pessoa.domain.PessoaPapel;
import br.com.rockgustavo.imobiliaria.pessoa.domain.UltimoAdministradorException;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaPapelRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaQueryRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaRepository;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaResumoView;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PessoaPapelRepository papelRepository;
    private final PessoaQueryRepository queryRepository;
    private final KeycloakAdminPort keycloakAdminPort;

    public PessoaService(PessoaRepository pessoaRepository, PessoaPapelRepository papelRepository,
                          PessoaQueryRepository queryRepository, KeycloakAdminPort keycloakAdminPort) {
        this.pessoaRepository = pessoaRepository;
        this.papelRepository = papelRepository;
        this.queryRepository = queryRepository;
        this.keycloakAdminPort = keycloakAdminPort;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UUID criar(CriarPessoaComando comando) {
        String documentoLimpo = comando.documento() == null ? null : comando.documento().replaceAll("\\D", "");
        if (documentoLimpo != null
                && pessoaRepository.existsByTipoDocumentoAndDocumento(comando.tipoDocumento(), documentoLimpo)) {
            throw new DocumentoDuplicadoException(comando.tipoDocumento(), comando.documento());
        }
        if (comando.email() != null && pessoaRepository.existsByEmail(comando.email())) {
            throw new EmailDuplicadoException(comando.email());
        }

        Pessoa pessoa = new Pessoa(comando.tipoDocumento(), comando.documento(), comando.nome(), comando.email());
        pessoaRepository.save(pessoa);
        return pessoa.getId();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public PessoaDetalhe buscarPorId(UUID id) {
        return paraDetalhe(buscarEntidade(id));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public PessoaDetalhe atualizar(AtualizarPessoaComando comando) {
        Pessoa pessoa = buscarEntidade(comando.pessoaId());
        if (comando.email() != null && !comando.email().equals(pessoa.getEmail())) {
            garantirEmailDisponivel(comando.email(), pessoa.getId());
        }
        pessoa.atualizar(comando.nome(), comando.email());
        return paraDetalhe(pessoa);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public PessoaDetalhe atribuirPapel(AtribuirPapelComando comando) {
        Pessoa pessoa = buscarEntidade(comando.pessoaId());
        if (papelRepository.existsByPessoaIdAndPapel(pessoa.getId(), comando.papel())) {
            throw new PapelJaAtribuidoException(comando.papel());
        }

        if (comando.papel() != Papel.PROPRIETARIO) {
            String email = comando.email() != null ? comando.email() : pessoa.getEmail();
            if (email == null || email.isBlank()) {
                throw new EmailObrigatorioException(comando.papel());
            }
            if (!email.equals(pessoa.getEmail())) {
                garantirEmailDisponivel(email, pessoa.getId());
                pessoa.atualizar(pessoa.getNome(), email);
            }
            if (pessoa.getSubjectIdp() == null) {
                String subjectIdp = keycloakAdminPort.provisionar(email, pessoa.getNome());
                pessoa.provisionarCredencial(subjectIdp);
            }
        }

        papelRepository.save(new PessoaPapel(pessoa, comando.papel()));
        return paraDetalhe(pessoa);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void removerPapel(UUID pessoaId, Papel papel) {
        Pessoa pessoa = buscarEntidade(pessoaId);
        PessoaPapel vinculo = papelRepository.findByPessoaIdAndPapel(pessoa.getId(), papel)
                .orElseThrow(() -> new PapelNaoAtribuidoException(papel));
        if (papel == Papel.ADMINISTRADOR) {
            garantirNaoUltimoAdministrador();
        }
        papelRepository.delete(vinculo);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void inativar(UUID pessoaId) {
        Pessoa pessoa = buscarEntidade(pessoaId);
        if (papelRepository.existsByPessoaIdAndPapel(pessoa.getId(), Papel.ADMINISTRADOR)) {
            garantirNaoUltimoAdministrador();
        }
        pessoa.inativar();
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<PessoaResumoView> listar(PessoaFiltro filtro, Pageable pageable) {
        if (filtro.classificacao() != null && filtro.classificacao() != ClassificacaoComercial.LEAD) {
            return Page.empty(pageable);
        }
        return queryRepository.listar(TenantContext.obter(), filtro, pageable);
    }

    private void garantirNaoUltimoAdministrador() {
        if (papelRepository.contarAdministradoresAtivos() <= 1) {
            throw new UltimoAdministradorException();
        }
    }

    private void garantirEmailDisponivel(String email, UUID pessoaId) {
        if (pessoaRepository.existsByEmailAndIdNot(email, pessoaId)) {
            throw new EmailDuplicadoException(email);
        }
    }

    private Pessoa buscarEntidade(UUID id) {
        return pessoaRepository.buscarPorId(id).orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    private PessoaDetalhe paraDetalhe(Pessoa pessoa) {
        List<Papel> papeis = papelRepository.findByPessoaId(pessoa.getId()).stream()
                .map(PessoaPapel::getPapel)
                .toList();
        return new PessoaDetalhe(pessoa.getId(), pessoa.getTipoDocumento(), pessoa.getDocumento(), pessoa.getNome(),
                pessoa.getEmail(), pessoa.isAtivo(), papeis, pessoa.getCriadoEm(), pessoa.getAlteradoEm());
    }
}
