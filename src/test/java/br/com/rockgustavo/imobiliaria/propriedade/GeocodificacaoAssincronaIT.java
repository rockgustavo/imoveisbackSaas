package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.AbstractIntegrationTest;
import br.com.rockgustavo.imobiliaria.shared.geo.TestGeoConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RN-03-08/04-02: geocodificação assíncrona de ponta a ponta")
class GeocodificacaoAssincronaIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("cadastro dispara geocodificação assíncrona que conclui a geolocalização")
    void cadastroDisparaGeocodificacaoAssincrona() throws Exception {
        UUID tenantId = fixture.criarTenant();
        UUID proprietarioId = fixture.criarProprietario(tenantId);
        UUID id = criarPropriedadeGeocodificavel(tenantId, proprietarioId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> mockMvc.perform(get("/api/v1/propriedades/{id}", id).with(administradorDoTenant(tenantId)))
                        .andExpect(jsonPath("$.geoSituacao").value("CONCLUIDA"))
                        .andExpect(jsonPath("$.latitude").value(-23.561684)));
    }

    private UUID criarPropriedadeGeocodificavel(UUID tenantId, UUID proprietarioId) throws Exception {
        String corpo = """
                {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                 "valorReferencia":450000.00,"cep":"%s","logradouro":"Rua Geocodificável","numero":"1",
                 "bairro":"Bairro","localidade":"Cidade","uf":"SP","enderecoValidado":true}
                """.formatted(proprietarioId, TestGeoConfig.CEP_GEOCODIFICAVEL);
        String location = mockMvc.perform(post("/api/v1/propriedades")
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
