package br.com.rockgustavo.imobiliaria.shared.geo;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class BrasilApiCepClient implements CepClient {

    private final RestClient restClient;

    public BrasilApiCepClient(@Value("${geo.cep.brasilapi-base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public CepConsulta consultar(String cep) {
        try {
            JsonNode corpo = restClient.get()
                    .uri("/api/cep/v2/{cep}", cep)
                    .retrieve()
                    .body(JsonNode.class);
            return paraCepConsulta(cep, corpo);
        } catch (HttpClientErrorException.NotFound e) {
            return CepConsulta.naoEncontrado(cep);
        } catch (RestClientException e) {
            throw new CepProvedorIndisponivelException(cep, e);
        }
    }

    private CepConsulta paraCepConsulta(String cep, JsonNode corpo) {
        if (corpo == null) {
            return CepConsulta.naoEncontrado(cep);
        }
        JsonNode coordenadas = corpo.path("location").path("coordinates");
        return new CepConsulta(
                cep, true,
                textoOuNulo(corpo, "street"),
                textoOuNulo(corpo, "neighborhood"),
                textoOuNulo(corpo, "city"),
                textoOuNulo(corpo, "state"),
                numeroOuNulo(coordenadas, "latitude"),
                numeroOuNulo(coordenadas, "longitude"));
    }

    private String textoOuNulo(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        return valor.isMissingNode() || valor.isNull() || valor.asText().isBlank() ? null : valor.asText();
    }

    private BigDecimal numeroOuNulo(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        if (valor.isMissingNode() || valor.isNull() || valor.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(valor.asText());
    }
}
