package br.com.rockgustavo.imobiliaria.painel.api;

import br.com.rockgustavo.imobiliaria.painel.application.PainelIndicadores;
import br.com.rockgustavo.imobiliaria.painel.application.PainelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/painel")
@Tag(name = "painel")
public class PainelController {

    private static final List<String> SITUACOES = List.of("DISPONIVEL", "AGENCIADA", "RESERVADA", "VENDIDA", "RETIRADA");
    private static final List<String> CLASSIFICACOES = List.of("LEAD", "PROSPECT", "CLIENTE", "CLIENTE_INATIVO");

    private final PainelService service;

    public PainelController(PainelService service) {
        this.service = service;
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Indicadores operacionais do tenant corrente",
            description = "RN-08-01..03. Comissão projetada soma apenas agenciamentos de contrato ATIVO com "
                    + "vigência corrente — é projeção, não receita realizada.")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados na data corrente do fuso do tenant")
    public PainelIndicadoresResponse indicadores() {
        return paraResponse(service.indicadores());
    }

    private static PainelIndicadoresResponse paraResponse(PainelIndicadores indicadores) {
        Map<String, Long> imoveisPorSituacao = comZerosPadrao(SITUACOES);
        indicadores.imoveisPorSituacao().forEach(v -> imoveisPorSituacao.put(v.situacao(), v.quantidade()));

        Map<String, Long> funilPorClassificacao = comZerosPadrao(CLASSIFICACOES);
        indicadores.funil().forEach(v -> funilPorClassificacao.put(v.classificacao(), v.quantidade()));

        FunilResponse funil = new FunilResponse(
                funilPorClassificacao.get("LEAD"),
                funilPorClassificacao.get("PROSPECT"),
                funilPorClassificacao.get("CLIENTE"),
                funilPorClassificacao.get("CLIENTE_INATIVO"));

        return new PainelIndicadoresResponse(
                indicadores.agregados().contratosAtivos(),
                indicadores.agregados().contratosVencendoEm30Dias(),
                imoveisPorSituacao,
                indicadores.agregados().orcamentosAguardandoResposta(),
                funil,
                indicadores.agregados().comissaoProjetada().toPlainString());
    }

    private static Map<String, Long> comZerosPadrao(List<String> chaves) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        chaves.forEach(chave -> mapa.put(chave, 0L));
        return mapa;
    }
}
