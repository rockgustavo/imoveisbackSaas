package br.com.rockgustavo.imobiliaria.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    static final String CLAIM_TENANT_ID = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolverTenant().ifPresent(TenantContext::definir);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }

    private Optional<UUID> resolverTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString(CLAIM_TENANT_ID);
            if (tenantId != null) {
                return Optional.of(UUID.fromString(tenantId));
            }
        }
        return Optional.empty();
    }
}
