package br.com.rockgustavo.imobiliaria.propriedade.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropriedadeTest {

    @Nested
    @DisplayName("RN-03-10: atributos mínimos")
    class Criacao {

        @Test
        @DisplayName("cria propriedade disponível com geolocalização pendente")
        void criaComDefaults() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();

            assertThat(propriedade.getId()).isNotNull();
            assertThat(propriedade.getSituacao()).isEqualTo(SituacaoPropriedade.DISPONIVEL);
            assertThat(propriedade.getGeoSituacao()).isEqualTo(GeoSituacao.PENDENTE);
            assertThat(propriedade.getGeoTentativas()).isZero();
            assertThat(propriedade.getLatitude()).isNull();
            assertThat(propriedade.getLongitude()).isNull();
        }

        @Test
        @DisplayName("rejeita valor de referência zero ou negativo")
        void rejeitaValorReferenciaInvalido() {
            assertThatThrownBy(() -> PropriedadeTestBuilder.umaPropriedade()
                    .comValorReferencia(BigDecimal.ZERO).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("permite área, quartos e vagas nulos")
        void permiteAtributosOpcionaisNulos() {
            Propriedade propriedade = new Propriedade(UUID.randomUUID(), TipoPropriedade.TERRENO, null, null, null,
                    new BigDecimal("100000.00"), PropriedadeTestBuilder.enderecoPadrao());

            assertThat(propriedade.getAreaPrivativa()).isNull();
            assertThat(propriedade.getQuartos()).isNull();
            assertThat(propriedade.getVagas()).isNull();
        }
    }

    @Nested
    @DisplayName("RN-03-06: transição delegada à situação atual")
    class TransicaoDeSituacao {

        @Test
        @DisplayName("transição válida aplica a nova situação")
        void transicaoValidaAplicaNovaSituacao() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();

            propriedade.transicionarPara(SituacaoPropriedade.AGENCIADA);

            assertThat(propriedade.getSituacao()).isEqualTo(SituacaoPropriedade.AGENCIADA);
        }

        @Test
        @DisplayName("transição inválida é rejeitada e não altera a situação")
        void transicaoInvalidaEhRejeitada() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();

            assertThatThrownBy(() -> propriedade.transicionarPara(SituacaoPropriedade.RESERVADA))
                    .isInstanceOf(PropriedadeTransicaoInvalidaException.class);
            assertThat(propriedade.getSituacao()).isEqualTo(SituacaoPropriedade.DISPONIVEL);
        }

        @Test
        @DisplayName("situação terminal não aceita nenhuma transição")
        void situacaoTerminalNaoAceitaTransicao() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.RETIRADA);

            assertThatThrownBy(() -> propriedade.transicionarPara(SituacaoPropriedade.DISPONIVEL))
                    .isInstanceOf(PropriedadeTransicaoInvalidaException.class);
        }

        @Test
        @DisplayName("desfazerReserva move reservada para agenciada")
        void desfazerReservaMoveReservadaParaAgenciada() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.AGENCIADA);
            propriedade.transicionarPara(SituacaoPropriedade.RESERVADA);

            propriedade.desfazerReserva();

            assertThat(propriedade.getSituacao()).isEqualTo(SituacaoPropriedade.AGENCIADA);
        }

        @Test
        @DisplayName("desfazerReserva rejeita quando não há reserva para desfazer, mesmo AGENCIADA sendo alcançável por outro caminho (via contrato)")
        void desfazerReservaRejeitaQuandoNaoHaReservaParaDesfazer() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();

            assertThatThrownBy(propriedade::desfazerReserva)
                    .isInstanceOf(PropriedadeTransicaoInvalidaException.class);
            assertThat(propriedade.getSituacao()).isEqualTo(SituacaoPropriedade.DISPONIVEL);
        }

        @Test
        @DisplayName("retirar, reservar e vender delegam para a situação correspondente")
        void metodosNomeadosDelegamParaSituacaoCorrespondente() {
            Propriedade paraRetirar = PropriedadeTestBuilder.umaPropriedade().build();
            paraRetirar.retirar();
            assertThat(paraRetirar.getSituacao()).isEqualTo(SituacaoPropriedade.RETIRADA);

            Propriedade paraReservar = PropriedadeTestBuilder.umaPropriedade().build();
            paraReservar.transicionarPara(SituacaoPropriedade.AGENCIADA);
            paraReservar.reservar();
            assertThat(paraReservar.getSituacao()).isEqualTo(SituacaoPropriedade.RESERVADA);

            Propriedade paraVender = PropriedadeTestBuilder.umaPropriedade().build();
            paraVender.transicionarPara(SituacaoPropriedade.AGENCIADA);
            paraVender.transicionarPara(SituacaoPropriedade.RESERVADA);
            paraVender.vender();
            assertThat(paraVender.getSituacao()).isEqualTo(SituacaoPropriedade.VENDIDA);
        }
    }

    @Nested
    @DisplayName("RN-03-07: propriedade com agenciamento vigente não troca de proprietário")
    class TrocaDeProprietario {

        @Test
        @DisplayName("permite trocar proprietário quando disponível")
        void permiteTrocaQuandoDisponivel() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            UUID novoProprietario = UUID.randomUUID();

            propriedade.trocarProprietario(novoProprietario);

            assertThat(propriedade.getProprietarioId()).isEqualTo(novoProprietario);
        }

        @Test
        @DisplayName("permite trocar proprietário quando retirada")
        void permiteTrocaQuandoRetirada() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.RETIRADA);
            UUID novoProprietario = UUID.randomUUID();

            propriedade.trocarProprietario(novoProprietario);

            assertThat(propriedade.getProprietarioId()).isEqualTo(novoProprietario);
        }

        @Test
        @DisplayName("rejeita troca quando agenciada")
        void rejeitaTrocaQuandoAgenciada() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.AGENCIADA);
            UUID proprietarioOriginal = propriedade.getProprietarioId();

            assertThatThrownBy(() -> propriedade.trocarProprietario(UUID.randomUUID()))
                    .isInstanceOf(PropriedadeComAgenciamentoVigenteException.class);
            assertThat(propriedade.getProprietarioId()).isEqualTo(proprietarioOriginal);
        }

        @Test
        @DisplayName("rejeita troca quando reservada")
        void rejeitaTrocaQuandoReservada() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.AGENCIADA);
            propriedade.transicionarPara(SituacaoPropriedade.RESERVADA);

            assertThatThrownBy(() -> propriedade.trocarProprietario(UUID.randomUUID()))
                    .isInstanceOf(PropriedadeComAgenciamentoVigenteException.class);
        }

        @Test
        @DisplayName("rejeita troca quando vendida")
        void rejeitaTrocaQuandoVendida() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.transicionarPara(SituacaoPropriedade.AGENCIADA);
            propriedade.transicionarPara(SituacaoPropriedade.RESERVADA);
            propriedade.transicionarPara(SituacaoPropriedade.VENDIDA);

            assertThatThrownBy(() -> propriedade.trocarProprietario(UUID.randomUUID()))
                    .isInstanceOf(PropriedadeComAgenciamentoVigenteException.class);
        }
    }

    @Nested
    @DisplayName("Atualização de dados cadastrais")
    class Atualizacao {

        @Test
        @DisplayName("atualiza tipo, atributos e endereço")
        void atualizaDadosCadastrais() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            Endereco novoEndereco = new Endereco("20040020", "Av. Rio Branco", "1", "sala 10", "Centro",
                    "Rio de Janeiro", "RJ", true);

            propriedade.atualizar(TipoPropriedade.CASA, new BigDecimal("120.00"), (short) 4, (short) 2,
                    new BigDecimal("900000.00"), novoEndereco);

            assertThat(propriedade.getTipo()).isEqualTo(TipoPropriedade.CASA);
            assertThat(propriedade.getAreaPrivativa()).isEqualByComparingTo("120.00");
            assertThat(propriedade.getQuartos()).isEqualTo((short) 4);
            assertThat(propriedade.getVagas()).isEqualTo((short) 2);
            assertThat(propriedade.getValorReferencia()).isEqualByComparingTo("900000.00");
            assertThat(propriedade.getEndereco()).isEqualTo(novoEndereco);
        }
    }
}
