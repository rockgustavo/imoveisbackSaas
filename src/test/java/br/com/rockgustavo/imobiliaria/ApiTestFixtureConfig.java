package br.com.rockgustavo.imobiliaria;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@TestConfiguration
public class ApiTestFixtureConfig {

    @Bean
    ApiTestFixture apiTestFixture(MockMvc mockMvc) {
        return new ApiTestFixture(mockMvc);
    }
}
