package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.TenantAtual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant")
@Tag(name = "imobiliaria")
public class TenantController {

    private final ImobiliariaService service;

    public TenantController(ImobiliariaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Identifica a imobiliária do token corrente",
            description = "Somente leitura — a identidade do tenant é definida no provisionamento (RN-00-06) e não tem endpoint de alteração")
    @ApiResponse(responseCode = "200", description = "Imobiliária do tenant corrente")
    @ApiResponse(responseCode = "401", description = "Sem token")
    @ApiResponse(responseCode = "403", description = "Sem papel USUARIO ou ADMINISTRADOR")
    @ApiResponse(responseCode = "404", description = "Tenant do token não existe (TENANT_NAO_ENCONTRADO)")
    public TenantResponse buscar() {
        TenantAtual tenant = service.buscarTenantAtual();
        return new TenantResponse(tenant.id(), tenant.razaoSocial(), tenant.slug(), tenant.status().name());
    }
}
