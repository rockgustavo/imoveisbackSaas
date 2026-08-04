package br.com.rockgustavo.imobiliaria.orcamento.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoTestBuilder.itemPadrao;
import static br.com.rockgustavo.imobiliaria.orcamento.domain.OrcamentoTestBuilder.umOrcamento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoTest {

    @Nested
    @DisplayName("RN-05-01: item carrega comissão e valor pedido")
    class Criacao {

        @Test
        @DisplayName("cria orçamento em RASCUNHO, versão 1, sem origem")
        void criaComDefaults() {
            Orcamento.ItemProposto item = itemPadrao();

            Orcamento orcamento = umOrcamento().comItens(List.of(item)).build();

            assertThat(orcamento.getId()).isNotNull();
            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(orcamento.getVersao()).isEqualTo(1);
            assertThat(orcamento.getOrigemId()).isNull();
            assertThat(orcamento.getItens()).hasSize(1);
            assertThat(orcamento.getItens().get(0).getPropriedadeId()).isEqualTo(item.propriedadeId());
            assertThat(orcamento.getItens().get(0).getComissaoPercentual()).isEqualByComparingTo(item.comissaoPercentual());
            assertThat(orcamento.getItens().get(0).getValorPedido()).isEqualByComparingTo(item.valorPedido());
        }

        @Test
        @DisplayName("rejeita item com comissão zero ou negativa")
        void rejeitaComissaoInvalida() {
            Orcamento.ItemProposto item = new Orcamento.ItemProposto(UUID.randomUUID(), BigDecimal.ZERO, new BigDecimal("100.00"));

            assertThatThrownBy(() -> umOrcamento().comItens(List.of(item)).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita item com valor pedido zero ou negativo")
        void rejeitaValorPedidoInvalido() {
            Orcamento.ItemProposto item = new Orcamento.ItemProposto(UUID.randomUUID(), new BigDecimal("6.00"), BigDecimal.ZERO);

            assertThatThrownBy(() -> umOrcamento().comItens(List.of(item)).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita propriedade repetida entre os itens")
        void rejeitaItemDuplicado() {
            UUID propriedadeId = UUID.randomUUID();
            List<Orcamento.ItemProposto> itens = List.of(
                    new Orcamento.ItemProposto(propriedadeId, new BigDecimal("6.00"), new BigDecimal("100.00")),
                    new Orcamento.ItemProposto(propriedadeId, new BigDecimal("5.00"), new BigDecimal("200.00")));

            assertThatThrownBy(() -> umOrcamento().comItens(itens).build())
                    .isInstanceOf(OrcamentoItemDuplicadoException.class);
        }
    }

    @Nested
    @DisplayName("RN-05-04: só é editável em RASCUNHO")
    class Edicao {

        @Test
        @DisplayName("atualiza itens por diff: mantém, remove e adiciona")
        void atualizaItensPorDiff() {
            UUID mantido = UUID.randomUUID();
            UUID removido = UUID.randomUUID();
            UUID adicionado = UUID.randomUUID();
            Orcamento orcamento = umOrcamento().comItens(List.of(
                    new Orcamento.ItemProposto(mantido, new BigDecimal("6.00"), new BigDecimal("100.00")),
                    new Orcamento.ItemProposto(removido, new BigDecimal("6.00"), new BigDecimal("100.00")))).build();

            orcamento.atualizar(List.of(
                    new Orcamento.ItemProposto(mantido, new BigDecimal("4.00"), new BigDecimal("150.00")),
                    new Orcamento.ItemProposto(adicionado, new BigDecimal("6.00"), new BigDecimal("200.00"))));

            assertThat(orcamento.getItens()).hasSize(2);
            assertThat(orcamento.getItens().stream().map(OrcamentoItem::getPropriedadeId))
                    .containsExactlyInAnyOrder(mantido, adicionado);
            OrcamentoItem itemMantido = orcamento.getItens().stream()
                    .filter(i -> i.getPropriedadeId().equals(mantido)).findFirst().orElseThrow();
            assertThat(itemMantido.getComissaoPercentual()).isEqualByComparingTo("4.00");
            assertThat(itemMantido.getValorPedido()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("rejeita atualização fora de RASCUNHO")
        void rejeitaAtualizacaoForaDeRascunho() {
            Orcamento orcamento = umOrcamento().build();
            orcamento.enviar();

            assertThatThrownBy(() -> orcamento.atualizar(List.of(itemPadrao())))
                    .isInstanceOf(OrcamentoNaoEditavelException.class);
        }

        @Test
        @DisplayName("enviar transiciona RASCUNHO para ENVIADO")
        void enviarTransicionaParaEnviado() {
            Orcamento orcamento = umOrcamento().build();

            orcamento.enviar();

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADO);
        }

        @Test
        @DisplayName("rejeita enviar quando já não está em RASCUNHO")
        void rejeitaEnviarForaDeRascunho() {
            Orcamento orcamento = umOrcamento().build();
            orcamento.enviar();

            assertThatThrownBy(orcamento::enviar).isInstanceOf(OrcamentoNaoEditavelException.class);
        }
    }

    @Nested
    @DisplayName("RN-05-07: duplicação cria nova versão independente")
    class Duplicacao {

        @Test
        @DisplayName("duplicar o original aponta origem para o próprio original")
        void duplicarOriginalApontaOrigemParaOriginal() {
            Orcamento original = umOrcamento().build();

            Orcamento duplicata = original.duplicar(LocalDate.now().plusDays(15), 2, original.getId());

            assertThat(duplicata.getId()).isNotEqualTo(original.getId());
            assertThat(duplicata.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
            assertThat(duplicata.getOrigemId()).isEqualTo(original.getId());
            assertThat(duplicata.getVersao()).isEqualTo(2);
        }

        @Test
        @DisplayName("duplicar uma duplicata mantém origem na raiz, não no intermediário")
        void duplicarUmaDuplicataMantemOrigemNaRaiz() {
            Orcamento original = umOrcamento().build();
            Orcamento v2 = original.duplicar(LocalDate.now().plusDays(15), 2, original.getId());

            Orcamento v3 = v2.duplicar(LocalDate.now().plusDays(15), 3, original.getId());

            assertThat(v3.getOrigemId()).isEqualTo(original.getId());
            assertThat(v3.getOrigemId()).isNotEqualTo(v2.getId());
            assertThat(v3.getVersao()).isEqualTo(3);
        }

        @Test
        @DisplayName("duplicata copia os itens do original, independentes")
        void duplicataCopiaItensDoOriginal() {
            Orcamento original = umOrcamento().comItens(List.of(itemPadrao(), itemPadrao())).build();

            Orcamento duplicata = original.duplicar(LocalDate.now().plusDays(15), 2, original.getId());
            duplicata.atualizar(List.of(itemPadrao()));

            assertThat(original.getItens()).hasSize(2);
            assertThat(duplicata.getItens()).hasSize(1);
        }

        @Test
        @DisplayName("duplicação é permitida a partir de qualquer status, original mantém seu status")
        void duplicacaoPermitidaDeQualquerStatusSemAlterarOriginal() {
            Orcamento original = umOrcamento().build();
            original.enviar();
            original.aceitar(LocalDate.now());

            Orcamento duplicata = original.duplicar(LocalDate.now().plusDays(15), 2, original.getId());

            assertThat(original.getStatus()).isEqualTo(StatusOrcamento.ACEITO);
            assertThat(duplicata.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
        }
    }

    @Nested
    @DisplayName("RN-05-03/05-05: aceitar e recusar exigem ENVIADO com validade vigente")
    class AceiteERecusa {

        @Test
        @DisplayName("aceita quando ENVIADO com validade não vencida")
        void aceitaQuandoDentroDaValidade() {
            Orcamento orcamento = umOrcamento().comValidade(LocalDate.now().plusDays(1)).build();
            orcamento.enviar();

            orcamento.aceitar(LocalDate.now());

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ACEITO);
        }

        @Test
        @DisplayName("recusa quando ENVIADO com validade não vencida")
        void recusaQuandoDentroDaValidade() {
            Orcamento orcamento = umOrcamento().comValidade(LocalDate.now().plusDays(1)).build();
            orcamento.enviar();

            orcamento.recusar(LocalDate.now());

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RECUSADO);
        }

        @Test
        @DisplayName("rejeita aceitar quando ainda é RASCUNHO")
        void rejeitaAceitarQuandoRascunho() {
            Orcamento orcamento = umOrcamento().build();

            assertThatThrownBy(() -> orcamento.aceitar(LocalDate.now()))
                    .isInstanceOf(OrcamentoTransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("rejeita aceitar quando ENVIADO mas com validade vencida, mesmo antes do job de expiração rodar")
        void rejeitaAceitarComValidadeVencida() {
            Orcamento orcamento = umOrcamento().comValidade(LocalDate.now().minusDays(1)).build();
            orcamento.enviar();

            assertThatThrownBy(() -> orcamento.aceitar(LocalDate.now()))
                    .isInstanceOf(OrcamentoTransicaoInvalidaException.class);
            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ENVIADO);
        }

        @Test
        @DisplayName("rejeita recusar quando ENVIADO mas com validade vencida")
        void rejeitaRecusarComValidadeVencida() {
            Orcamento orcamento = umOrcamento().comValidade(LocalDate.now().minusDays(1)).build();
            orcamento.enviar();

            assertThatThrownBy(() -> orcamento.recusar(LocalDate.now()))
                    .isInstanceOf(OrcamentoTransicaoInvalidaException.class);
        }
    }

    @Nested
    @DisplayName("RN-05-03: expiração automática")
    class Expiracao {

        @Test
        @DisplayName("expira orçamento ENVIADO")
        void expiraQuandoEnviado() {
            Orcamento orcamento = umOrcamento().build();
            orcamento.enviar();

            orcamento.expirar();

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.EXPIRADO);
        }

        @Test
        @DisplayName("não altera orçamento que já foi respondido — idempotente sob corrida com aceite/recusa")
        void naoAlteraOrcamentoJaRespondido() {
            Orcamento orcamento = umOrcamento().comValidade(LocalDate.now().plusDays(1)).build();
            orcamento.enviar();
            orcamento.aceitar(LocalDate.now());

            orcamento.expirar();

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.ACEITO);
        }

        @Test
        @DisplayName("não altera orçamento ainda em RASCUNHO")
        void naoAlteraOrcamentoEmRascunho() {
            Orcamento orcamento = umOrcamento().build();

            orcamento.expirar();

            assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
        }
    }
}
