package br.com.rockgustavo.imobiliaria.propriedade.api;

import br.com.rockgustavo.imobiliaria.propriedade.application.MapaFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.application.MapaResultado;
import br.com.rockgustavo.imobiliaria.propriedade.application.MapaService;
import br.com.rockgustavo.imobiliaria.propriedade.application.StatusContratoFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.domain.BoundingBox;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.MapaPropriedadeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mapa")
@Tag(name = "propriedade")
public class MapaController {

    private final MapaService service;

    public MapaController(MapaService service) {
        this.service = service;
    }

    @GetMapping("/propriedades")
    @Operation(summary = "Lista propriedades geocodificadas do tenant dentro de um bounding box",
            description = "RN-07-01..05. Sem paginação tradicional: RN-07-04 substitui page/size por bounding box "
                    + "e teto fixo de 500 resultados — acima disso, o campo `limitado` sinaliza que a resposta foi cortada. "
                    + "`situacao` é repetível (`?situacao=DISPONIVEL&situacao=AGENCIADA`) para combinar mais de uma "
                    + "situação na mesma consulta; ausente, vale o default de RN-07-03.")
    @ApiResponse(responseCode = "200", description = "Propriedades dentro do bounding box, já filtradas")
    @ApiResponse(responseCode = "400", description = "bbox ausente, malformado ou com min >= max (BOUNDING_BOX_INVALIDO); "
            + "ou valor de enum inválido em situacao/statusContrato (PARAMETRO_INVALIDO)")
    public MapaResponse listar(
            @Parameter(example = "-23.60,-46.70,-23.50,-46.60", description = "minLat,minLon,maxLat,maxLon")
            @RequestParam String bbox,
            @RequestParam(required = false) List<SituacaoPropriedade> situacao,
            @RequestParam(required = false) StatusContratoFiltro statusContrato,
            @RequestParam(required = false) @Schema(example = "100000.00") BigDecimal valorMin,
            @RequestParam(required = false) @Schema(example = "900000.00") BigDecimal valorMax,
            @RequestParam(required = false) String localidade,
            @RequestParam(required = false) @Schema(example = "SP") String uf,
            @RequestParam(required = false) UUID proprietarioId) {
        MapaFiltro filtro = new MapaFiltro(BoundingBox.parse(bbox), situacao, statusContrato, valorMin, valorMax,
                localidade, uf, proprietarioId);
        MapaResultado resultado = service.buscar(filtro);
        return paraResponse(resultado);
    }

    private static MapaResponse paraResponse(MapaResultado resultado) {
        return new MapaResponse(resultado.propriedades().stream().map(MapaController::paraPropriedadeResponse).toList(),
                resultado.limitado());
    }

    private static MapaPropriedadeResponse paraPropriedadeResponse(MapaPropriedadeView view) {
        return new MapaPropriedadeResponse(
                view.id(), view.proprietarioId(), view.situacao().name(), view.valorReferencia().toPlainString(),
                view.logradouro(), view.localidade(), view.uf(), view.latitude(), view.longitude(),
                view.statusContrato() == null ? null : view.statusContrato().name());
    }
}
