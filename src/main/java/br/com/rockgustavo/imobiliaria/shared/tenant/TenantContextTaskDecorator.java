package br.com.rockgustavo.imobiliaria.shared.tenant;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        UUID tenantId = TenantContext.obter();
        return () -> {
            TenantContext.definir(tenantId);
            try {
                runnable.run();
            } finally {
                TenantContext.limpar();
            }
        };
    }
}
