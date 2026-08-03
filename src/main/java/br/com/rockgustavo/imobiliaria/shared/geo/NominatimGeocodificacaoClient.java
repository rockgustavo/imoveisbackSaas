package br.com.rockgustavo.imobiliaria.shared.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class NominatimGeocodificacaoClient implements GeocodificacaoClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodificacaoClient.class);

    private final RestClient restClient;
    private final String userAgent;

    public NominatimGeocodificacaoClient(
            @Value("${geo.geocodificacao.nominatim-base-url}") String baseUrl,
            @Value("${geo.geocodificacao.user-agent}") String userAgent) {
        this.restClient = RestClient.create(baseUrl);
        this.userAgent = userAgent;
    }

    @Override
    public Optional<Coordenada> geocodificar(EnderecoParaGeocodificar endereco) {
        try {
            List<NominatimResultado> resultados = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .queryParam("countrycodes", "br")
                            .queryParam("street", endereco.logradouro() + ", " + endereco.numero())
                            .queryParam("city", endereco.localidade())
                            .queryParam("state", endereco.uf())
                            .queryParam("postalcode", endereco.cep())
                            .build())
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NominatimResultado>>() {
                    });
            return resultados == null || resultados.isEmpty()
                    ? Optional.empty()
                    : Optional.of(resultados.get(0).paraCoordenada());
        } catch (RestClientException e) {
            log.warn("Falha ao geocodificar endereço no Nominatim: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private record NominatimResultado(String lat, String lon) {

        Coordenada paraCoordenada() {
            return new Coordenada(new BigDecimal(lat), new BigDecimal(lon));
        }
    }
}
