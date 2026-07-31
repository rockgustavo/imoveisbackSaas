package br.com.rockgustavo.imobiliaria.shared.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    static final String CLAIM_PESSOA_ID = "pessoa_id";

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String pessoaId = jwt.getClaimAsString(CLAIM_PESSOA_ID);
            if (pessoaId != null) {
                return Optional.of(UUID.fromString(pessoaId));
            }
        }
        return Optional.of(SistemaAutor.ID);
    }
}
