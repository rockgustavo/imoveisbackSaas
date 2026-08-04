package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.domain.Orcamento;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoComissaoAcimaDoTetoException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoPropriedadeIndisponivelException;
import br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoTestBuilder;
import br.com.rockgustavo.imobiliaria.orcamento.domain.PessoaInativaNaoRecebeOrcamentoException;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoQueryRepository;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoRepository;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    OrcamentoRepository orcamentoRepository;

    @Mock
    OrcamentoQueryRepository queryRepository;

    @Mock
    PessoaFacade pessoaFacade;

    @Mock
    PropriedadeFacade propriedadeFacade;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    private final UUID tenantId = UUID.randomUUID();

    private OrcamentoService novoService() {
        return new OrcamentoService(orcamentoRepository, queryRepository, pessoaFacade, propriedadeFacade, imobiliariaFacade);
    }

    @BeforeEach
    void definirTenant() {
        TenantContext.definir(tenantId);
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Nested
    @DisplayName("RN-01-06: pessoa inativa não recebe orçamento")
    class PessoaInativa {

        @Test
        @DisplayName("rejeita criação para pessoa inativa")
        void rejeitaCriacaoParaPessoaInativa() {
            UUID pessoaId = UUID.randomUUID();
            when(pessoaFacade.estaAtiva(pessoaId)).thenReturn(false);
            CriarOrcamentoComando comando = new CriarOrcamentoComando(pessoaId,
                    List.of(new ItemComando(UUID.randomUUID(), new BigDecimal("6.00"), new BigDecimal("100.00"))));

            assertThatThrownBy(() -> novoService().criar(comando))
                    .isInstanceOf(PessoaInativaNaoRecebeOrcamentoException.class);
        }
    }

    @Nested
    @DisplayName("Criação valida existência da propriedade referenciada")
    class CriacaoComPropriedadeInexistente {

        @Test
        @DisplayName("rejeita item cuja propriedade não existe no tenant")
        void rejeitaPropriedadeInexistente() {
            UUID pessoaId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(pessoaFacade.estaAtiva(pessoaId)).thenReturn(true);
            when(propriedadeFacade.existe(propriedadeId)).thenReturn(false);
            CriarOrcamentoComando comando = new CriarOrcamentoComando(pessoaId,
                    List.of(new ItemComando(propriedadeId, new BigDecimal("6.00"), new BigDecimal("100.00"))));

            assertThatThrownBy(() -> novoService().criar(comando))
                    .isInstanceOf(OrcamentoPropriedadeIndisponivelException.class);
        }
    }

    @Nested
    @DisplayName("RN-05-08: envio exige posse e disponibilidade da propriedade, e comissão dentro do teto")
    class Envio {

        @Test
        @DisplayName("rejeita envio quando propriedade não está disponível para o proprietário do orçamento")
        void rejeitaEnvioComPropriedadeIndisponivel() {
            Orcamento orcamento = OrcamentoTestBuilder.umOrcamento().build();
            when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(new BigDecimal("10.00"));
            when(propriedadeFacade.disponivelParaAgenciamentoPor(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> novoService().enviar(orcamento.getId()))
                    .isInstanceOf(OrcamentoPropriedadeIndisponivelException.class);
        }

        @Test
        @DisplayName("rejeita envio quando comissão do item excede o teto do tenant")
        void rejeitaEnvioComComissaoAcimaDoTeto() {
            Orcamento orcamento = OrcamentoTestBuilder.umOrcamento().build();
            when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(propriedadeFacade.disponivelParaAgenciamentoPor(any(), any())).thenReturn(true);
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(new BigDecimal("5.00"));

            assertThatThrownBy(() -> novoService().enviar(orcamento.getId()))
                    .isInstanceOf(OrcamentoComissaoAcimaDoTetoException.class);
        }

        @Test
        @DisplayName("envia quando propriedade disponível e comissão dentro do teto")
        void enviaQuandoValido() {
            Orcamento orcamento = OrcamentoTestBuilder.umOrcamento().build();
            when(orcamentoRepository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
            when(propriedadeFacade.disponivelParaAgenciamentoPor(any(), any())).thenReturn(true);
            when(imobiliariaFacade.comissaoPercentualTeto(tenantId)).thenReturn(new BigDecimal("10.00"));

            OrcamentoDetalhe detalhe = novoService().enviar(orcamento.getId());

            assertThat(detalhe.status().name()).isEqualTo("ENVIADO");
        }
    }

    @Nested
    @DisplayName("Cálculo de validade a partir do parâmetro do tenant")
    class CalculoDeValidade {

        @Test
        @DisplayName("validade da criação é hoje-no-fuso-do-tenant + dias padrão")
        void calculaValidadeNaCriacao() {
            UUID pessoaId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(pessoaFacade.estaAtiva(pessoaId)).thenReturn(true);
            when(propriedadeFacade.existe(propriedadeId)).thenReturn(true);
            when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
            when(imobiliariaFacade.orcamentoValidadeDiasPadrao(tenantId)).thenReturn(15);
            CriarOrcamentoComando comando = new CriarOrcamentoComando(pessoaId,
                    List.of(new ItemComando(propriedadeId, new BigDecimal("6.00"), new BigDecimal("100.00"))));

            novoService().criar(comando);

            ArgumentCaptor<Orcamento> captor = ArgumentCaptor.forClass(Orcamento.class);
            verify(orcamentoRepository).save(captor.capture());
            assertThat(captor.getValue().getValidade())
                    .isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(15));
        }
    }

    @Nested
    @DisplayName("RN-05-07: duplicação calcula versão a partir da raiz")
    class Duplicacao {

        @Test
        @DisplayName("duplicar orçamento original usa o próprio id como raiz e versão seguinte")
        void duplicarOriginalUsaProprioIdComoRaiz() {
            Orcamento original = OrcamentoTestBuilder.umOrcamento().build();
            when(orcamentoRepository.buscarPorId(original.getId())).thenReturn(Optional.of(original));
            when(orcamentoRepository.maiorVersaoPorOrigem(original.getId())).thenReturn(1);
            when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
            when(imobiliariaFacade.orcamentoValidadeDiasPadrao(tenantId)).thenReturn(15);

            novoService().duplicar(original.getId());

            ArgumentCaptor<Orcamento> captor = ArgumentCaptor.forClass(Orcamento.class);
            verify(orcamentoRepository).save(captor.capture());
            assertThat(captor.getValue().getOrigemId()).isEqualTo(original.getId());
            assertThat(captor.getValue().getVersao()).isEqualTo(2);
        }
    }
}
