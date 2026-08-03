package br.com.rockgustavo.imobiliaria.shared.security;

import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AcessoAtivoCache {

    private static final Duration TTL = Duration.ofSeconds(5);

    private final AcessoPort acessoPort;
    private final ConcurrentHashMap<String, Entrada> cache = new ConcurrentHashMap<>();

    public AcessoAtivoCache(AcessoPort acessoPort) {
        this.acessoPort = acessoPort;
    }

    public boolean estaAtivo(String subjectIdp) {
        String chave = chave(TenantContext.obter(), subjectIdp);
        Entrada atual = cache.get(chave);
        Instant agora = Instant.now();
        if (atual != null && agora.isBefore(atual.expiraEm())) {
            return atual.ativo();
        }
        boolean ativo = acessoPort.estaAtivo(subjectIdp);
        cache.put(chave, new Entrada(ativo, agora.plus(TTL)));
        return ativo;
    }

    public void invalidar(String subjectIdp) {
        if (subjectIdp != null) {
            cache.remove(chave(TenantContext.obter(), subjectIdp));
        }
    }

    private static String chave(UUID tenantId, String subjectIdp) {
        return tenantId + ":" + subjectIdp;
    }

    private record Entrada(boolean ativo, Instant expiraEm) {
    }
}
