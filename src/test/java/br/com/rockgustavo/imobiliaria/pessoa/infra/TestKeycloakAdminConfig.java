package br.com.rockgustavo.imobiliaria.pessoa.infra;

import br.com.rockgustavo.imobiliaria.pessoa.application.KeycloakAdminPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

@TestConfiguration
public class TestKeycloakAdminConfig {

    @Bean
    @Primary
    public KeycloakAdminPort keycloakAdminPort() {
        return TestKeycloakAdminConfig::subjectIdpDeterministico;
    }

    public static String subjectIdpDeterministico(String email, String nome, UUID tenantId) {
        return "fake-subject-" + tenantId + "-" + email;
    }
}
