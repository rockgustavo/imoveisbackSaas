package br.com.rockgustavo.imobiliaria;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class OpenApiExportIT extends AbstractIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exportaOpenApiJson() throws Exception {
        String corpo = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode arvore = objectMapper.readTree(corpo);
        String formatado = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arvore);

        Path destino = Path.of("docs", "api", "openapi.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, formatado + System.lineSeparator());
    }
}
