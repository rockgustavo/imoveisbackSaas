package br.com.rockgustavo.imobiliaria.contrato.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.contrato.domain.ContratoTestBuilder.itemPadrao;
import static br.com.rockgustavo.imobiliaria.contrato.domain.ContratoTestBuilder.umContrato;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContratoTest {

    private static final BigDecimal TETO_ALTO = new BigDecimal("20.00");

    @Nested
    @DisplayName("RN-06-01/02: vigência e quantidade mínima de propriedades")
    class Criacao {

        @Test
        @DisplayName("cria contrato em RASCUNHO com agenciamentos inativos")
        void criaComDefaults() {
            Contrato.ItemParaAgenciar item = itemPadrao();

            Contrato contrato = umContrato().comItens(List.of(item)).build();

            assertThat(contrato.getId()).isNotNull();
            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.RASCUNHO);
            assertThat(contrato.getAgenciamentos()).hasSize(1);
            Agenciamento agenciamento = contrato.getAgenciamentos().get(0);
            assertThat(agenciamento.getPropriedadeId()).isEqualTo(item.propriedadeId());
            assertThat(agenciamento.getComissaoPercentual()).isEqualByComparingTo(item.comissaoPercentual());
            assertThat(agenciamento.getValorPedido()).isEqualByComparingTo(item.valorPedido());
            assertThat(agenciamento.isContratoAtivo()).isFalse();
        }

        @Test
        @DisplayName("rejeita vigência com fim anterior ou igual ao início")
        void rejeitaVigenciaInvalida() {
            LocalDate data = LocalDate.now();

            assertThatThrownBy(() -> umContrato().comVigencia(data, data).build())
                    .isInstanceOf(ContratoVigenciaInvalidaException.class);
            assertThatThrownBy(() -> umContrato().comVigencia(data, data.minusDays(1)).build())
                    .isInstanceOf(ContratoVigenciaInvalidaException.class);
        }

        @Test
        @DisplayName("RN-06-02: nunca chega sem propriedade — orçamento de origem já garante ao menos um item")
        void exigeAoMenosUmItem() {
            assertThatThrownBy(() -> umContrato().comItens(List.of()).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("RN-06-11/12: ativação copia valores do orçamento e revalida o teto")
    class Ativacao {

        @Test
        @DisplayName("ativa: status ATIVO e todos os agenciamentos ficam ativos — RN-06-05 não é checado aqui, é responsabilidade do Service+banco (ADR-17)")
        void ativaContratoRascunho() {
            Contrato contrato = umContrato().comItens(List.of(itemPadrao(), itemPadrao())).build();

            contrato.ativar(TETO_ALTO);

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ATIVO);
            assertThat(contrato.getAgenciamentos()).allMatch(Agenciamento::isContratoAtivo);
        }

        @Test
        @DisplayName("rejeita ativação de contrato que não está RASCUNHO")
        void rejeitaAtivacaoForaDeRascunho() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            assertThatThrownBy(() -> contrato.ativar(TETO_ALTO))
                    .isInstanceOf(ContratoTransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("rejeita ativação quando comissão de algum item excede o teto atual do tenant")
        void rejeitaComissaoAcimaDoTeto() {
            Contrato.ItemParaAgenciar item = new Contrato.ItemParaAgenciar(
                    UUID.randomUUID(), new BigDecimal("10.00"), new BigDecimal("100.00"));
            Contrato contrato = umContrato().comItens(List.of(item)).build();

            assertThatThrownBy(() -> contrato.ativar(new BigDecimal("5.00")))
                    .isInstanceOf(ContratoComissaoAcimaDoTetoException.class);
            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.RASCUNHO);
        }
    }

    @Nested
    @DisplayName("RN-06-04: encerramento e cancelamento")
    class EncerramentoECancelamento {

        @Test
        @DisplayName("encerra contrato ATIVO com justificativa e libera os agenciamentos")
        void encerraContratoAtivo() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            contrato.encerrar("distrato antecipado por acordo entre as partes");

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.ENCERRADO);
            assertThat(contrato.getJustificativaEncerramento()).isNotBlank();
            assertThat(contrato.getAgenciamentos()).noneMatch(Agenciamento::isContratoAtivo);
        }

        @Test
        @DisplayName("rejeita encerramento sem justificativa")
        void rejeitaEncerramentoSemJustificativa() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            assertThatThrownBy(() -> contrato.encerrar(" ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita encerramento de contrato que não está ATIVO")
        void rejeitaEncerramentoForaDeAtivo() {
            Contrato contrato = umContrato().build();

            assertThatThrownBy(() -> contrato.encerrar("motivo"))
                    .isInstanceOf(ContratoTransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("cancela contrato RASCUNHO sem checagem de propriedade")
        void cancelaContratoRascunho() {
            Contrato contrato = umContrato().build();

            contrato.cancelar();

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.CANCELADO);
        }

        @Test
        @DisplayName("cancela contrato ATIVO e libera os agenciamentos")
        void cancelaContratoAtivo() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            contrato.cancelar();

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.CANCELADO);
            assertThat(contrato.getAgenciamentos()).noneMatch(Agenciamento::isContratoAtivo);
        }

        @Test
        @DisplayName("rejeita cancelamento de contrato já ENCERRADO")
        void rejeitaCancelamentoDeEncerrado() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);
            contrato.encerrar("motivo");

            assertThatThrownBy(contrato::cancelar).isInstanceOf(ContratoTransicaoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("RN-06-09: expiração automática")
    class Expiracao {

        @Test
        @DisplayName("expira contrato ATIVO e libera os agenciamentos")
        void expiraContratoAtivo() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            contrato.expirar();

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.EXPIRADO);
            assertThat(contrato.getAgenciamentos()).noneMatch(Agenciamento::isContratoAtivo);
        }

        @Test
        @DisplayName("não altera contrato que já não está ATIVO — idempotente sob corrida com o job")
        void naoAlteraContratoQueNaoEstaAtivo() {
            Contrato contrato = umContrato().build();

            contrato.expirar();

            assertThat(contrato.getStatus()).isEqualTo(StatusContrato.RASCUNHO);
        }
    }

    @Nested
    @DisplayName("RN-06-08/13: aditivo de inclusão, exclusão e renegociação")
    class Aditivos {

        @Test
        @DisplayName("inclui propriedade nova em contrato ATIVO")
        void incluiPropriedadeNova() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);
            UUID novaPropriedade = UUID.randomUUID();

            contrato.incluirPropriedade(novaPropriedade, new BigDecimal("6.00"), new BigDecimal("300000.00"),
                    TETO_ALTO, "inclusão de novo imóvel ao contrato");

            assertThat(contrato.getAgenciamentos()).hasSize(2);
            assertThat(contrato.possuiAgenciamentoAtivoPara(novaPropriedade)).isTrue();
            assertThat(contrato.getAditivos()).hasSize(1);
            assertThat(contrato.getAditivos().get(0).getTipo()).isEqualTo(TipoAditivo.INCLUSAO);
        }

        @Test
        @DisplayName("renegocia comissão/valor de propriedade já agenciada sem soltar a propriedade")
        void renegociaSemSoltarPropriedade() {
            Contrato.ItemParaAgenciar item = itemPadrao();
            Contrato contrato = umContrato().comItens(List.of(item)).build();
            contrato.ativar(TETO_ALTO);

            contrato.incluirPropriedade(item.propriedadeId(), new BigDecimal("8.00"), new BigDecimal("500000.00"),
                    TETO_ALTO, "renegociação de comissão");

            assertThat(contrato.getAgenciamentos()).hasSize(1);
            Agenciamento agenciamento = contrato.getAgenciamentos().get(0);
            assertThat(agenciamento.isContratoAtivo()).isTrue();
            assertThat(agenciamento.getComissaoPercentual()).isEqualByComparingTo("8.00");
            assertThat(agenciamento.getValorPedido()).isEqualByComparingTo("500000.00");
            assertThat(contrato.getAditivos()).hasSize(2);
            assertThat(contrato.getAditivos().stream().map(Aditivo::getTipo))
                    .containsExactlyInAnyOrder(TipoAditivo.EXCLUSAO, TipoAditivo.INCLUSAO);
        }

        @Test
        @DisplayName("rejeita inclusão com comissão acima do teto")
        void rejeitaInclusaoAcimaDoTeto() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            assertThatThrownBy(() -> contrato.incluirPropriedade(UUID.randomUUID(), new BigDecimal("50.00"),
                    new BigDecimal("100.00"), TETO_ALTO, "justificativa"))
                    .isInstanceOf(ContratoComissaoAcimaDoTetoException.class);
        }

        @Test
        @DisplayName("exclui propriedade agenciada")
        void excluiPropriedadeAgenciada() {
            Contrato.ItemParaAgenciar item = itemPadrao();
            Contrato contrato = umContrato().comItens(List.of(item)).build();
            contrato.ativar(TETO_ALTO);

            contrato.excluirPropriedade(item.propriedadeId(), "exclusão a pedido do proprietário");

            assertThat(contrato.getAgenciamentos()).isEmpty();
            assertThat(contrato.getAditivos()).hasSize(1);
            assertThat(contrato.getAditivos().get(0).getTipo()).isEqualTo(TipoAditivo.EXCLUSAO);
        }

        @Test
        @DisplayName("rejeita exclusão de propriedade que não está agenciada neste contrato")
        void rejeitaExclusaoDePropriedadeNaoAgenciada() {
            Contrato contrato = umContrato().build();
            contrato.ativar(TETO_ALTO);

            assertThatThrownBy(() -> contrato.excluirPropriedade(UUID.randomUUID(), "motivo"))
                    .isInstanceOf(ContratoAgenciamentoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("rejeita aditivo em contrato que não está ATIVO")
        void rejeitaAditivoForaDeAtivo() {
            Contrato contrato = umContrato().build();

            assertThatThrownBy(() -> contrato.incluirPropriedade(UUID.randomUUID(), new BigDecimal("6.00"),
                    new BigDecimal("100.00"), TETO_ALTO, "motivo"))
                    .isInstanceOf(ContratoTransicaoInvalidaException.class);
        }
    }
}
