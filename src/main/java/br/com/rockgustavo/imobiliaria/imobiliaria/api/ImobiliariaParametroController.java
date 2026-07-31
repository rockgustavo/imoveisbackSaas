package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.AtualizarParametrosComando;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaParametroService;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ParametrosTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/parametros")
@Tag(name = "imobiliaria")
public class ImobiliariaParametroController {

    private final ImobiliariaParametroService service;

    public ImobiliariaParametroController(ImobiliariaParametroService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Parâmetros configuráveis do tenant corrente", description = "RN-00-09")
    public ParametrosResponse buscar() {
        return paraResponse(service.buscarDoTenantAtual());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualiza parâmetros do tenant", description = "RN-00-09/10 — não retroage a contrato/orçamento já existente")
    public ParametrosResponse atualizar(@Valid @RequestBody AtualizarParametrosRequest request) {
        AtualizarParametrosComando comando = new AtualizarParametrosComando(
                request.comissaoPercentualTeto(),
                request.orcamentoValidadeDiasPadrao(),
                request.geocodificacaoTentativasMax(),
                request.cepCacheJanelaDias(),
                request.fusoHorario());
        return paraResponse(service.atualizar(comando));
    }

    private ParametrosResponse paraResponse(ParametrosTenant parametros) {
        return new ParametrosResponse(
                parametros.comissaoPercentualTeto().toPlainString(),
                parametros.orcamentoValidadeDiasPadrao(),
                parametros.geocodificacaoTentativasMax(),
                parametros.cepCacheJanelaDias(),
                parametros.fusoHorario());
    }
}
