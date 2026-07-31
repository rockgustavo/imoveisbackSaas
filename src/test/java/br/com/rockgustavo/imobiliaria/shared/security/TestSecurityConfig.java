package br.com.rockgustavo.imobiliaria.shared.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] chave = new byte[32];
        new SecureRandom().nextBytes(chave);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(chave, "HmacSHA256")).build();
    }
}
