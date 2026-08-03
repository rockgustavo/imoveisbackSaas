package br.com.rockgustavo.imobiliaria.shared.security;

import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AcessoAtivoInterceptor implements HandlerInterceptor {

    private final AcessoAtivoCache cache;

    public AcessoAtivoInterceptor(AcessoAtivoCache cache) {
        this.cache = cache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.obter() == null) {
            return true;
        }
        boolean bloqueado = AutenticacaoAtual.subjectIdp()
                .map(subjectIdp -> !cache.estaAtivo(subjectIdp))
                .orElse(false);
        if (bloqueado) {
            throw new AcessoRevogadoException();
        }
        return true;
    }
}
