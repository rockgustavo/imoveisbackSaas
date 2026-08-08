package br.com.rockgustavo.imobiliaria;

import br.com.rockgustavo.imobiliaria.pessoa.infra.TestKeycloakAdminConfig;
import br.com.rockgustavo.imobiliaria.shared.geo.TestGeoConfig;
import br.com.rockgustavo.imobiliaria.shared.security.TestSecurityConfig;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestKeycloakAdminConfig.class, TestGeoConfig.class, ApiTestFixtureConfig.class})
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ApiTestFixture fixture;

    @Autowired
    protected EventPublicationRegistry eventPublicationRegistry;

    protected void aguardarEventosDeAuditoriaAssentarem() {
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(eventPublicationRegistry.findIncompletePublications()).isEmpty());
    }
}
