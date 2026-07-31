package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImobiliariaParametroTest {

    private final UUID tenantId = UUID.randomUUID();

    @Nested
    @DisplayName("RN-00-09: defaults de plataforma")
    class Defaults {

        @Test
        @DisplayName("nasce com os valores default documentados")
        void nasceComDefaults() {
            ImobiliariaParametro parametro = ImobiliariaParametro.comDefaultsDePlataforma(tenantId);

            assertThat(parametro.getTenantId()).isEqualTo(tenantId);
            assertThat(parametro.getComissaoPercentualTeto()).isEqualByComparingTo("6.00");
            assertThat(parametro.getOrcamentoValidadeDiasPadrao()).isEqualTo(15);
            assertThat(parametro.getGeocodificacaoTentativasMax()).isEqualTo((short) 5);
            assertThat(parametro.getCepCacheJanelaDias()).isEqualTo(30);
            assertThat(parametro.getFusoHorario()).isEqualTo("America/Sao_Paulo");
        }
    }

    @Nested
    @DisplayName("RN-06-12: teto de comissão maior que zero")
    class Atualizacao {

        @Test
        @DisplayName("rejeita teto de comissão zero")
        void rejeitaTetoZero() {
            ImobiliariaParametro parametro = ImobiliariaParametro.comDefaultsDePlataforma(tenantId);

            assertThatThrownBy(() -> parametro.atualizar(BigDecimal.ZERO, 15, (short) 5, 30, "America/Sao_Paulo"))
                    .isInstanceOf(ParametroInvalidoException.class);
        }

        @Test
        @DisplayName("rejeita fuso horário em branco")
        void rejeitaFusoEmBranco() {
            ImobiliariaParametro parametro = ImobiliariaParametro.comDefaultsDePlataforma(tenantId);

            assertThatThrownBy(() -> parametro.atualizar(new BigDecimal("6.00"), 15, (short) 5, 30, " "))
                    .isInstanceOf(ParametroInvalidoException.class);
        }

        @Test
        @DisplayName("aceita atualização válida")
        void aceitaAtualizacaoValida() {
            ImobiliariaParametro parametro = ImobiliariaParametro.comDefaultsDePlataforma(tenantId);

            parametro.atualizar(new BigDecimal("8.00"), 20, (short) 3, 45, "America/Fortaleza");

            assertThat(parametro.getComissaoPercentualTeto()).isEqualByComparingTo("8.00");
            assertThat(parametro.getOrcamentoValidadeDiasPadrao()).isEqualTo(20);
            assertThat(parametro.getGeocodificacaoTentativasMax()).isEqualTo((short) 3);
            assertThat(parametro.getCepCacheJanelaDias()).isEqualTo(45);
            assertThat(parametro.getFusoHorario()).isEqualTo("America/Fortaleza");
        }
    }
}
