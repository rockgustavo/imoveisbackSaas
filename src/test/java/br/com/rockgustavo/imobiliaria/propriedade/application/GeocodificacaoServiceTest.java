package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.GeoSituacao;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeTestBuilder;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import br.com.rockgustavo.imobiliaria.shared.geo.Coordenada;
import br.com.rockgustavo.imobiliaria.shared.geo.GeocodificacaoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeocodificacaoServiceTest {

    @Mock
    PropriedadeRepository propriedadeRepository;

    @Mock
    GeocodificacaoClient geocodificacaoClient;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    private GeocodificacaoService novoService() {
        return new GeocodificacaoService(propriedadeRepository, geocodificacaoClient, imobiliariaFacade);
    }

    @Nested
    @DisplayName("RN-04-02: geocodificação com sucesso")
    class Sucesso {

        @Test
        @DisplayName("coordenada válida conclui a geolocalização")
        void coordenadaValidaConcluiGeolocalizacao() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            when(propriedadeRepository.buscarPorId(propriedade.getId())).thenReturn(Optional.of(propriedade));
            when(geocodificacaoClient.geocodificar(any()))
                    .thenReturn(Optional.of(new Coordenada(new BigDecimal("-23.561684"), new BigDecimal("-46.655981"))));

            novoService().tentarGeocodificar(propriedade.getId());

            assertThat(propriedade.getGeoSituacao()).isEqualTo(GeoSituacao.CONCLUIDA);
            assertThat(propriedade.getLatitude()).isEqualByComparingTo("-23.561684");
            assertThat(propriedade.getLongitude()).isEqualByComparingTo("-46.655981");
        }
    }

    @Nested
    @DisplayName("RN-04-04: falha do fornecedor não invalida o cadastro")
    class FalhaDoFornecedor {

        @Test
        @DisplayName("sem coordenada, permanece PENDENTE e soma uma tentativa")
        void semCoordenadaPermanecePendente() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            when(propriedadeRepository.buscarPorId(propriedade.getId())).thenReturn(Optional.of(propriedade));
            when(geocodificacaoClient.geocodificar(any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.geocodificacaoTentativasMax(any())).thenReturn((short) 5);

            novoService().tentarGeocodificar(propriedade.getId());

            assertThat(propriedade.getGeoSituacao()).isEqualTo(GeoSituacao.PENDENTE);
            assertThat(propriedade.getGeoTentativas()).isEqualTo((short) 1);
        }
    }

    @Nested
    @DisplayName("RN-04-05: limite de tentativas marca MANUAL")
    class LimiteDeTentativas {

        @Test
        @DisplayName("ao atingir o teto do tenant, situação vira MANUAL")
        void atingirTetoMarcaManual() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            when(propriedadeRepository.buscarPorId(propriedade.getId())).thenReturn(Optional.of(propriedade));
            when(geocodificacaoClient.geocodificar(any())).thenReturn(Optional.empty());
            when(imobiliariaFacade.geocodificacaoTentativasMax(any())).thenReturn((short) 1);

            novoService().tentarGeocodificar(propriedade.getId());

            assertThat(propriedade.getGeoSituacao()).isEqualTo(GeoSituacao.MANUAL);
            assertThat(propriedade.getGeoTentativas()).isEqualTo((short) 1);
        }
    }

    @Nested
    @DisplayName("RN-04-06: coordenada fora da bounding box nacional")
    class ForaDaBoundingBox {

        @Test
        @DisplayName("coordenada fora do território nacional é tratada como falha, não é persistida")
        void coordenadaForaDoTerritorioEhTratadaComoFalha() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            when(propriedadeRepository.buscarPorId(propriedade.getId())).thenReturn(Optional.of(propriedade));
            when(geocodificacaoClient.geocodificar(any()))
                    .thenReturn(Optional.of(new Coordenada(new BigDecimal("40.7128"), new BigDecimal("-74.0060"))));
            when(imobiliariaFacade.geocodificacaoTentativasMax(any())).thenReturn((short) 5);

            novoService().tentarGeocodificar(propriedade.getId());

            assertThat(propriedade.getGeoSituacao()).isEqualTo(GeoSituacao.PENDENTE);
            assertThat(propriedade.getLatitude()).isNull();
            assertThat(propriedade.getGeoTentativas()).isEqualTo((short) 1);
        }
    }

    @Nested
    @DisplayName("Idempotência")
    class Idempotencia {

        @Test
        @DisplayName("propriedade inexistente não gera erro nem chamada ao fornecedor")
        void propriedadeInexistenteNaoChamaFornecedor() {
            UUID id = UUID.randomUUID();
            when(propriedadeRepository.buscarPorId(id)).thenReturn(Optional.empty());

            novoService().tentarGeocodificar(id);

            verify(geocodificacaoClient, never()).geocodificar(any());
        }

        @Test
        @DisplayName("propriedade já concluída não chama o fornecedor de novo")
        void propriedadeJaConcluidaNaoChamaFornecedorDeNovo() {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade().build();
            propriedade.concluirGeolocalizacao(new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
            when(propriedadeRepository.buscarPorId(propriedade.getId())).thenReturn(Optional.of(propriedade));

            novoService().tentarGeocodificar(propriedade.getId());

            verify(geocodificacaoClient, never()).geocodificar(any());
        }
    }
}
