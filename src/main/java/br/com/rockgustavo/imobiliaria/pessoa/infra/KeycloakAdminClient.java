package br.com.rockgustavo.imobiliaria.pessoa.infra;

import br.com.rockgustavo.imobiliaria.pessoa.application.KeycloakAdminPort;
import br.com.rockgustavo.imobiliaria.pessoa.domain.CredencialProvisionamentoException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KeycloakAdminClient implements KeycloakAdminPort {

    private static final Duration MARGEM_DE_RENOVACAO = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final AtomicReference<TokenEmCache> tokenEmCache = new AtomicReference<>();

    public KeycloakAdminClient(
            @Value("${keycloak.admin.server-url}") String serverUrl,
            @Value("${keycloak.admin.realm}") String realm,
            @Value("${keycloak.admin.client-id}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.restClient = RestClient.create(serverUrl);
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String provisionar(String email, String nome) {
        String token = obterToken(email);
        try {
            ResponseEntity<Void> resposta = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NovoUsuarioKeycloak(email, email, primeiroNome(nome), sobrenome(nome), true, true))
                    .retrieve()
                    .toBodilessEntity();
            return extrairId(resposta, email);
        } catch (HttpClientErrorException.Conflict e) {
            return buscarPorEmail(token, email);
        } catch (RestClientException e) {
            throw new CredencialProvisionamentoException(email, e);
        }
    }

    private String obterToken(String email) {
        TokenEmCache cache = tokenEmCache.get();
        if (cache != null && cache.utilizavelEm(Instant.now())) {
            return cache.valor();
        }
        TokenEmCache renovado = solicitarToken(email);
        tokenEmCache.set(renovado);
        return renovado.valor();
    }

    private TokenEmCache solicitarToken(String email) {
        try {
            MultiValueMap<String, String> corpo = new LinkedMultiValueMap<>();
            corpo.add("grant_type", "client_credentials");
            corpo.add("client_id", clientId);
            corpo.add("client_secret", clientSecret);

            TokenResponse resposta = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(corpo)
                    .retrieve()
                    .body(TokenResponse.class);
            if (resposta == null || resposta.accessToken() == null) {
                throw new CredencialProvisionamentoException(email, null);
            }
            return TokenEmCache.de(resposta.accessToken(), resposta.expiraEmSegundos());
        } catch (RestClientException e) {
            throw new CredencialProvisionamentoException(email, e);
        }
    }

    private String buscarPorEmail(String token, String email) {
        List<UsuarioKeycloak> usuarios = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/admin/realms/{realm}/users")
                        .queryParam("email", email)
                        .queryParam("exact", true)
                        .build(realm))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<UsuarioKeycloak>>() {
                });
        return usuarios == null ? null : usuarios.stream()
                .findFirst()
                .map(UsuarioKeycloak::id)
                .orElseThrow(() -> new CredencialProvisionamentoException(email, null));
    }

    private String extrairId(ResponseEntity<Void> resposta, String email) {
        URI location = resposta.getHeaders().getLocation();
        if (location == null) {
            throw new CredencialProvisionamentoException(email, null);
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String primeiroNome(String nome) {
        return nome.trim().split("\\s+", 2)[0];
    }

    private String sobrenome(String nome) {
        String[] partes = nome.trim().split("\\s+", 2);
        return partes.length > 1 ? partes[1] : "";
    }

    private record NovoUsuarioKeycloak(
            String username, String email, String firstName, String lastName, boolean enabled, boolean emailVerified) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiraEmSegundos) {
    }

    private record TokenEmCache(String valor, Instant expiraEm) {

        static TokenEmCache de(String valor, Long expiraEmSegundos) {
            long segundos = expiraEmSegundos == null ? 0L : expiraEmSegundos;
            return new TokenEmCache(valor, Instant.now().plusSeconds(segundos).minus(MARGEM_DE_RENOVACAO));
        }

        boolean utilizavelEm(Instant momento) {
            return momento.isBefore(expiraEm);
        }
    }

    private record UsuarioKeycloak(String id) {
    }
}
