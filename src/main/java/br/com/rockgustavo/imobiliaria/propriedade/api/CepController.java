package br.com.rockgustavo.imobiliaria.propriedade.api;

import br.com.rockgustavo.imobiliaria.propriedade.application.CepService;
import br.com.rockgustavo.imobiliaria.shared.geo.CepConsulta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ceps")
@Tag(name = "propriedade")
public class CepController {

    private final CepService service;

    public CepController(CepService service) {
        this.service = service;
    }

    @GetMapping("/{cep}")
    @Operation(summary = "Consulta endereço por CEP", description = "RN-03-02/04, RN-04-01/03 — resultado cacheado pela janela do tenant")
    @ApiResponse(responseCode = "200", description = "Consulta concluída — verificar campo encontrado; CEP inexistente não é erro (CEP_NAO_ENCONTRADO)")
    @ApiResponse(responseCode = "502", description = "Fornecedor de CEP indisponível (CEP_PROVEDOR_INDISPONIVEL)")
    public CepResponse consultar(@PathVariable @Schema(example = "01310100") @Parameter(description = "8 dígitos, com ou sem máscara") String cep) {
        CepConsulta consulta = service.consultar(cep);
        return new CepResponse(
                consulta.cep(), consulta.encontrado(), consulta.logradouro(), consulta.bairro(),
                consulta.localidade(), consulta.uf(), consulta.latitude(), consulta.longitude());
    }
}
