package br.com.rockgustavo.imobiliaria.shared.geo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@TestConfiguration
public class TestGeoConfig {

    public static final String CEP_NAO_ENCONTRADO = "00000000";
    public static final String CEP_PROVEDOR_INDISPONIVEL = "11111111";
    public static final String CEP_GEOCODIFICAVEL = "01000000";

    private static final AtomicInteger CHAMADAS_AO_FORNECEDOR = new AtomicInteger();

    @Bean
    @Primary
    public CepClient cepClient() {
        return cep -> {
            if (CEP_PROVEDOR_INDISPONIVEL.equals(cep)) {
                throw new CepProvedorIndisponivelException(cep, null);
            }
            if (CEP_NAO_ENCONTRADO.equals(cep)) {
                return CepConsulta.naoEncontrado(cep);
            }
            int chamada = CHAMADAS_AO_FORNECEDOR.incrementAndGet();
            return new CepConsulta(cep, true, "Rua Teste " + chamada, "Bairro Teste", "Cidade Teste", "SP", null, null);
        };
    }

    @Bean
    @Primary
    public GeocodificacaoClient geocodificacaoClient() {
        return endereco -> CEP_GEOCODIFICAVEL.equals(endereco.cep())
                ? Optional.of(new Coordenada(new BigDecimal("-23.561684"), new BigDecimal("-46.655981")))
                : Optional.empty();
    }
}
