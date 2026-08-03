package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.shared.geo.CepCacheRepository;
import br.com.rockgustavo.imobiliaria.shared.geo.CepClient;
import br.com.rockgustavo.imobiliaria.shared.geo.CepConsulta;
import br.com.rockgustavo.imobiliaria.shared.geo.CepTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CepServiceTest {

    @Mock
    CepClient cepClient;

    @Mock
    CepCacheRepository cacheRepository;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    @InjectMocks
    CepService service;

    @Nested
    @DisplayName("RN-04-03: janela de cache")
    class JanelaDeCache {

        @Test
        @DisplayName("cache válido não consulta o fornecedor")
        void cacheValidoNaoConsultaFornecedor() {
            String cep = CepTestFixture.novoCep();
            CepConsulta cacheado = new CepConsulta(cep, true, "Rua Cacheada", "Bairro", "Cidade", "SP", null, null);
            when(imobiliariaFacade.cepCacheJanelaDias(any())).thenReturn(30);
            when(cacheRepository.buscarValido(eq(cep), anyInt())).thenReturn(Optional.of(cacheado));

            CepConsulta resultado = service.consultar(cep);

            assertThat(resultado.logradouro()).isEqualTo("Rua Cacheada");
            verify(cepClient, never()).consultar(any());
        }

        @Test
        @DisplayName("sem cache válido, consulta o fornecedor e armazena o resultado")
        void semCacheValidoConsultaFornecedorEArmazena() {
            String cep = CepTestFixture.novoCep();
            CepConsulta doFornecedor = new CepConsulta(cep, true, "Rua Nova", "Bairro", "Cidade", "SP",
                    new BigDecimal("-23.5"), new BigDecimal("-46.6"));
            when(imobiliariaFacade.cepCacheJanelaDias(any())).thenReturn(30);
            when(cacheRepository.buscarValido(eq(cep), anyInt())).thenReturn(Optional.empty());
            when(cepClient.consultar(cep)).thenReturn(doFornecedor);

            CepConsulta resultado = service.consultar(cep);

            assertThat(resultado).isEqualTo(doFornecedor);
            verify(cacheRepository).salvar(doFornecedor);
        }
    }

    @Nested
    @DisplayName("CEP mal formado")
    class CepMalFormado {

        @Test
        @DisplayName("não consulta cache nem fornecedor")
        void naoConsultaCacheNemFornecedor() {
            CepConsulta resultado = service.consultar("123");

            assertThat(resultado.encontrado()).isFalse();
            verify(cepClient, never()).consultar(any());
            verify(cacheRepository, never()).buscarValido(any(), anyInt());
        }
    }
}
