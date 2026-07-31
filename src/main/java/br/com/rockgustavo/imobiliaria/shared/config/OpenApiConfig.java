package br.com.rockgustavo.imobiliaria.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH2 = "oauth2";

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public OpenAPI imobiliariaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SaaS de administração de corretora de imóveis")
                        .description("Ver docs/convencoes-api.md no repositório para o contrato completo.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH2))
                .components(new Components().addSecuritySchemes(OAUTH2,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(issuerUri + "/protocol/openid-connect/auth")
                                                .tokenUrl(issuerUri + "/protocol/openid-connect/token")
                                                .scopes(new Scopes()
                                                        .addString("openid", "OpenID Connect")
                                                        .addString("profile", "Perfil")
                                                        .addString("email", "Email"))))));
    }
}
