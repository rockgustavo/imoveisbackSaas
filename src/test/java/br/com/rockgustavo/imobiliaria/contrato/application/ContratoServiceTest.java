package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.Contrato;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoCancelamentoInviavelException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoComissaoAcimaDoTetoException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoPropriedadeIndisponivelException;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoTestBuilder;
import br.com.rockgustavo.imobiliaria.contrato.domain.ContratoVigenciaSobrepostaException;
import br.com.rockgustavo.imobiliaria.contrato.domain.OrcamentoJaOriginouContratoException;
import br.com.rockgustavo.imobiliaria.contrato.domain.PropriedadeProprietarioDivergenteException;
import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;
import br.com.rockgustavo.imobiliaria.contrato.infra.ConflitoVigenciaView;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoHistoricoQueryRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoQueryRepository;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoRepository;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade.ItemAceito;
import br.com.rockgustavo.imobiliaria.orcamento.OrcamentoFacade.OrcamentoAceitoDetalhe;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    private static final BigDecimal TETO_ALTO = new BigDecimal("20.00");

    @Mock
    ContratoRepository contratoRepository;

    @Mock
    ContratoQueryRepository queryRepository;

    @Mock
    ContratoHistoricoService contratoHistoricoService;

    @Mock
    ContratoHistoricoQueryRepository contratoHistoricoQueryRepository;

    @Mock
    OrcamentoFacade orcamentoFacade;

    @Mock
    PropriedadeFacade propriedadeFacade;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    AuditorAware<UUID> auditorAware;

    private final UUID tenantId = UUID.randomUUID();

    private ContratoService novoService() {
        return new ContratoService(contratoRepository, queryRepository, contratoHistoricoService,
                contratoHistoricoQueryRepository, orcamentoFacade, propriedadeFacade, imobiliariaFacade,
                eventPublisher, auditorAware);
    }

    @BeforeEach
    void definirTenant() {
        TenantContext.definir(tenantId);
        lenient().when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Nested
    @DisplayName("RN-05-05/06, RN-06-06: criação a partir de orçamento aceito")
    class Criacao {

        @Test
        @DisplayName("cria contrato copiando pessoa e itens do orçamento aceito")
        void criaContratoAPartirDeOrcamentoAceito() {
            UUID orcamentoId = UUID.randomUUID();
            UUID pessoaId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(contratoRepository.existsByOrcamentoOrigemId(orcamentoId)).thenReturn(false);
            when(orcamentoFacade.buscarAceito(orcamentoId)).thenReturn(new OrcamentoAceitoDetalhe(pessoaId,
                    List.of(new ItemAceito(propriedadeId, new BigDecimal("6.00"), new BigDecimal("450000.00")))));
            when(propriedadeFacade.pertenceAoProprietario(propriedadeId, pessoaId)).thenReturn(true);
            CriarContratoComando comando = new CriarContratoComando(orcamentoId, LocalDate.now(),
                    LocalDate.now().plusYears(1), "regras contratuais");

            novoService().criar(comando);

            ArgumentCaptor<Contrato> captor = ArgumentCaptor.forClass(Contrato.class);
            verify(contratoRepository).save(captor.capture());
            assertThat(captor.getValue().getPessoaId()).isEqualTo(pessoaId);
            assertThat(captor.getValue().getOrcamentoOrigemId()).isEqualTo(orcamentoId);
            assertThat(captor.getValue().getAgenciamentos()).hasSize(1);
        }

        @Test
        @DisplayName("RN-05-06: rejeita quando o orçamento já originou outro contrato")
        void rejeitaOrcamentoJaOriginouContrato() {
            UUID orcamentoId = UUID.randomUUID();
            when(contratoRepository.existsByOrcamentoOrigemId(orcamentoId)).thenReturn(true);
            CriarContratoComando comando = new CriarContratoComando(orcamentoId, LocalDate.now(),
                    LocalDate.now().plusYears(1), "regras");

            assertThatThrownBy(() -> novoService().criar(comando))
                    .isInstanceOf(OrcamentoJaOriginouContratoException.class);
        }

        @Test
        @DisplayName("RN-06-06: rejeita quando propriedade do orçamento não pertence mais ao proprietário")
        void rejeitaPropriedadeDeOutroProprietario() {
            UUID orcamentoId = UUID.randomUUID();
            UUID pessoaId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(contratoRepository.existsByOrcamentoOrigemId(orcamentoId)).thenReturn(false);
            when(orcamentoFacade.buscarAceito(orcamentoId)).thenReturn(new OrcamentoAceitoDetalhe(pessoaId,
                    List.of(new ItemAceito(propriedadeId, new BigDecimal("6.00"), new BigDecimal("450000.00")))));
            when(propriedadeFacade.pertenceAoProprietario(propriedadeId, pessoaId)).thenReturn(false);
            CriarContratoComando comando = new CriarContratoComando(orcamentoId, LocalDate.now(),
                    LocalDate.now().plusYears(1), "regras");

            assertThatThrownBy(() -> novoService().criar(comando))
                    .isInstanceOf(PropriedadeProprietarioDivergenteException.class);
        }
    }

    @Nested
    @DisplayName("RN-06-05/07/12: ativação")
    class Ativacao {

        @Test
        @DisplayName("rejeita ativação quando propriedade não está disponível para o proprietário do contrato")
        void rejeitaAtivacaoComPropriedadeIndisponivel() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> novoService().ativar(contrato.getId()))
                    .isInstanceOf(ContratoPropriedadeIndisponivelException.class);
        }

        @Test
        @DisplayName("rejeita ativação quando pré-check encontra contrato conflitante com vigência sobreposta")
        void rejeitaAtivacaoComConflitoDetectadoNoPreCheck() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any()))
                    .thenReturn(Optional.of(new ConflitoVigenciaView(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(6))));

            assertThatThrownBy(() -> novoService().ativar(contrato.getId()))
                    .isInstanceOf(ContratoVigenciaSobrepostaException.class);
        }

        @Test
        @DisplayName("rejeita ativação quando comissão excede o teto atual do tenant")
        void rejeitaAtivacaoComComissaoAcimaDoTeto() {
            Contrato contrato = ContratoTestBuilder.umContrato()
                    .comItens(List.of(new Contrato.ItemParaAgenciar(UUID.randomUUID(), new BigDecimal("10.00"), new BigDecimal("100.00"))))
                    .build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(new BigDecimal("5.00"));

            assertThatThrownBy(() -> novoService().ativar(contrato.getId()))
                    .isInstanceOf(ContratoComissaoAcimaDoTetoException.class);
            verify(propriedadeFacade, never()).agenciar(any());
        }

        @Test
        @DisplayName("ativa e move propriedades para AGENCIADA quando válido")
        void ativaQuandoValido() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);

            ContratoDetalhe detalhe = novoService().ativar(contrato.getId());

            assertThat(detalhe.status().name()).isEqualTo("ATIVO");
            verify(propriedadeFacade).agenciar(any());
        }

        @Test
        @DisplayName("sob corrida: EXCLUDE do banco estoura no flush e é traduzido em ContratoVigenciaSobrepostaException")
        void traduzExcecaoDeExclusaoDoBancoNoFlush() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            ConstraintViolationException causa = new ConstraintViolationException(
                    "conflito", new SQLException("erro"), "agenciamento_vigencia_excl");
            when(contratoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("erro", causa));

            assertThatThrownBy(() -> novoService().ativar(contrato.getId()))
                    .isInstanceOf(ContratoVigenciaSobrepostaException.class);
            verify(propriedadeFacade, never()).agenciar(any());
        }

        @Test
        @DisplayName("RN-06-05: sob corrida simultânea o EXCLUDE causa deadlock no banco, também traduzido em ContratoVigenciaSobrepostaException")
        void traduzDeadlockDaExclusaoNoFlush() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            when(contratoRepository.saveAndFlush(any()))
                    .thenThrow(new CannotAcquireLockException("deadlock detected while checking exclusion constraint"));

            assertThatThrownBy(() -> novoService().ativar(contrato.getId()))
                    .isInstanceOf(ContratoVigenciaSobrepostaException.class);
            verify(propriedadeFacade, never()).agenciar(any());
        }

        @Test
        @DisplayName("violação de integridade não relacionada à exclusividade de vigência propaga sem tradução")
        void naoTraduzOutraViolacaoDeIntegridade() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            ConstraintViolationException causa = new ConstraintViolationException(
                    "outra", new SQLException("erro"), "outra_constraint_qualquer");
            DataIntegrityViolationException excecaoOriginal = new DataIntegrityViolationException("erro", causa);
            when(contratoRepository.saveAndFlush(any())).thenThrow(excecaoOriginal);

            assertThatThrownBy(() -> novoService().ativar(contrato.getId())).isSameAs(excecaoOriginal);
        }
    }

    @Nested
    @DisplayName("RN-06-10: cancelamento")
    class Cancelamento {

        @Test
        @DisplayName("cancela contrato RASCUNHO sem checar propriedades")
        void cancelaRascunhoSemChecarPropriedades() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));

            ContratoDetalhe detalhe = novoService().cancelar(contrato.getId());

            assertThat(detalhe.status().name()).isEqualTo("CANCELADO");
            verify(propriedadeFacade, never()).semNegociacaoEmAndamento(any());
        }

        @Test
        @DisplayName("rejeita cancelamento de ATIVO com propriedade RESERVADA ou VENDIDA")
        void rejeitaCancelamentoComNegociacaoEmAndamento() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            novoService().ativar(contrato.getId());
            when(propriedadeFacade.semNegociacaoEmAndamento(any())).thenReturn(false);

            assertThatThrownBy(() -> novoService().cancelar(contrato.getId()))
                    .isInstanceOf(ContratoCancelamentoInviavelException.class);
        }

        @Test
        @DisplayName("cancela ATIVO sem negociação em andamento e libera as propriedades")
        void cancelaAtivoLiberandoPropriedades() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            novoService().ativar(contrato.getId());
            when(propriedadeFacade.semNegociacaoEmAndamento(any())).thenReturn(true);

            ContratoDetalhe detalhe = novoService().cancelar(contrato.getId());

            assertThat(detalhe.status().name()).isEqualTo("CANCELADO");
            verify(propriedadeFacade).liberarAgenciamento(any());
        }
    }

    @Nested
    @DisplayName("RN-06-08/13: aditivo — inclusão nova versus renegociação")
    class Aditivo {

        @Test
        @DisplayName("inclusão de propriedade nova move a propriedade para AGENCIADA")
        void inclusaoNovaAgenciaPropriedade() {
            Contrato contrato = ContratoTestBuilder.umContrato().build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            novoService().ativar(contrato.getId());
            UUID novaPropriedade = UUID.randomUUID();
            AditivoComando comando = new AditivoComando(contrato.getId(), TipoAditivo.INCLUSAO, novaPropriedade,
                    "inclusão", new BigDecimal("6.00"), new BigDecimal("300000.00"));

            novoService().registrarAditivo(comando);

            verify(propriedadeFacade).agenciar(novaPropriedade);
        }

        @Test
        @DisplayName("renegociação de propriedade já agenciada não toca em PropriedadeFacade")
        void renegociacaoNaoTocaPropriedadeFacade() {
            Contrato.ItemParaAgenciar item = ContratoTestBuilder.itemPadrao();
            Contrato contrato = ContratoTestBuilder.umContrato().comItens(List.of(item)).build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            novoService().ativar(contrato.getId());
            AditivoComando comando = new AditivoComando(contrato.getId(), TipoAditivo.INCLUSAO, item.propriedadeId(),
                    "renegociação", new BigDecimal("8.00"), new BigDecimal("500000.00"));

            novoService().registrarAditivo(comando);

            verify(propriedadeFacade, times(1)).agenciar(any());
            verify(propriedadeFacade, never()).liberarAgenciamento(any());
        }

        @Test
        @DisplayName("exclusão de propriedade agenciada libera a propriedade")
        void exclusaoLiberaPropriedade() {
            Contrato.ItemParaAgenciar item = ContratoTestBuilder.itemPadrao();
            Contrato contrato = ContratoTestBuilder.umContrato().comItens(List.of(item)).build();
            when(contratoRepository.buscarPorId(contrato.getId())).thenReturn(Optional.of(contrato));
            when(propriedadeFacade.podeSerAgenciadaPor(any(), any())).thenReturn(true);
            when(queryRepository.buscarConflito(eq(tenantId), any(), any(), any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(TETO_ALTO);
            novoService().ativar(contrato.getId());
            AditivoComando comando = new AditivoComando(contrato.getId(), TipoAditivo.EXCLUSAO, item.propriedadeId(),
                    "exclusão", null, null);

            novoService().registrarAditivo(comando);

            verify(propriedadeFacade).liberarAgenciamento(item.propriedadeId());
        }
    }
}
