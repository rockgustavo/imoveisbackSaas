package br.com.rockgustavo.imobiliaria.propriedade.api;

import br.com.rockgustavo.imobiliaria.propriedade.application.AtualizarPropriedadeComando;
import br.com.rockgustavo.imobiliaria.propriedade.application.CriarPropriedadeComando;
import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeDetalhe;
import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeFiltro;
import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeService;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeResumoView;
import br.com.rockgustavo.imobiliaria.shared.geo.Coordenada;
import br.com.rockgustavo.imobiliaria.shared.geo.EnderecoParaGeocodificar;
import br.com.rockgustavo.imobiliaria.shared.web.PageResponse;
import br.com.rockgustavo.imobiliaria.shared.web.PaginacaoSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/propriedades")
@Tag(name = "propriedade")
public class PropriedadeController {

    private final PropriedadeService service;

    public PropriedadeController(PropriedadeService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma propriedade", description = "RN-03-01..05")
    @ApiResponse(responseCode = "201", description = "Propriedade criada")
    @ApiResponse(responseCode = "422", description = "Proprietário inválido — precisa estar ativo e ter papel PROPRIETARIO (PROPRIEDADE_PROPRIETARIO_INVALIDO)")
    public ResponseEntity<Void> criar(@Valid @RequestBody CriarPropriedadeRequest request) {
        UUID id = service.criar(new CriarPropriedadeComando(
                request.proprietarioId(), request.tipo(), request.areaPrivativa(), request.quartos(),
                request.vagas(), request.valorReferencia(), request.cep(), request.logradouro(), request.numero(),
                request.complemento(), request.bairro(), request.localidade(), request.uf(),
                request.enderecoValidado()));
        return ResponseEntity.created(URI.create("/api/v1/propriedades/" + id)).build();
    }

    @GetMapping
    @Operation(summary = "Lista propriedades do tenant", description = "RN-03-05")
    public PageResponse<PropriedadeResumoResponse> listar(
            @RequestParam(required = false) SituacaoPropriedade situacao,
            @RequestParam(required = false) UUID proprietarioId,
            @RequestParam(required = false) String localidade,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @PageableDefault(size = 20) Pageable pageable) {
        PaginacaoSupport.validar(pageable);
        PropriedadeFiltro filtro = new PropriedadeFiltro(situacao, proprietarioId, localidade, uf, valorMin, valorMax);
        Page<PropriedadeResumoView> pagina = service.listar(filtro, pageable);
        return PageResponse.de(pagina.map(PropriedadeController::paraResumoResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma propriedade")
    @ApiResponse(responseCode = "404", description = "Propriedade não encontrada, ou de outro tenant (PROPRIEDADE_NAO_ENCONTRADA)")
    public PropriedadeResponse buscar(@PathVariable UUID id) {
        return paraResponse(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados cadastrais da propriedade", description = "RN-03-07: troca de proprietário rejeitada com agenciamento vigente")
    @ApiResponse(responseCode = "422", description = "Proprietário inválido, ou agenciamento vigente bloqueia a troca (PROPRIEDADE_PROPRIETARIO_INVALIDO, PROPRIEDADE_COM_AGENCIAMENTO_VIGENTE)")
    public PropriedadeResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarPropriedadeRequest request) {
        AtualizarPropriedadeComando comando = new AtualizarPropriedadeComando(
                id, request.proprietarioId(), request.tipo(), request.areaPrivativa(), request.quartos(),
                request.vagas(), request.valorReferencia(), request.cep(), request.logradouro(), request.numero(),
                request.complemento(), request.bairro(), request.localidade(), request.uf(),
                request.enderecoValidado(), request.latitude(), request.longitude());
        return paraResponse(service.atualizar(comando));
    }

    @PostMapping("/geolocalizacao/pesquisar")
    @Operation(summary = "Pesquisa a coordenada geográfica de um endereço, sem persistir",
            description = "Usado para corrigir manualmente uma propriedade com geoSituacao=MANUAL: o front busca a "
                    + "coordenada e, se encontrada, envia junto no PUT de atualização")
    @ApiResponse(responseCode = "200", description = "Pesquisa concluída — 'encontrada' indica se houve resultado")
    public PesquisarGeolocalizacaoResponse pesquisarGeolocalizacao(
            @Valid @RequestBody PesquisarGeolocalizacaoRequest request) {
        Optional<Coordenada> coordenada = service.pesquisarGeolocalizacao(new EnderecoParaGeocodificar(
                request.cep(), request.logradouro(), request.numero(), request.bairro(), request.localidade(),
                request.uf()));
        return coordenada.map(PesquisarGeolocalizacaoResponse::encontrada)
                .orElseGet(PesquisarGeolocalizacaoResponse::naoEncontrada);
    }

    @PostMapping("/{id}/retirada")
    @Operation(summary = "Retira a propriedade de disponibilidade", description = "RN-03-06")
    @ApiResponse(responseCode = "422", description = "Transição inválida a partir da situação atual (PROPRIEDADE_TRANSICAO_INVALIDA)")
    public PropriedadeResponse retirar(@PathVariable UUID id) {
        return paraResponse(service.retirar(id));
    }

    @PostMapping("/{id}/reserva")
    @Operation(summary = "Reserva a propriedade agenciada", description = "RN-03-06")
    @ApiResponse(responseCode = "422", description = "Transição inválida a partir da situação atual (PROPRIEDADE_TRANSICAO_INVALIDA)")
    public PropriedadeResponse reservar(@PathVariable UUID id) {
        return paraResponse(service.reservar(id));
    }

    @DeleteMapping("/{id}/reserva")
    @Operation(summary = "Desfaz a reserva, devolvendo a propriedade para agenciada", description = "RN-03-06")
    @ApiResponse(responseCode = "422", description = "Transição inválida a partir da situação atual (PROPRIEDADE_TRANSICAO_INVALIDA)")
    public PropriedadeResponse desfazerReserva(@PathVariable UUID id) {
        return paraResponse(service.desfazerReserva(id));
    }

    @PostMapping("/{id}/venda")
    @Operation(summary = "Marca a propriedade como vendida", description = "RN-03-06/11 — marcação manual, sem dado financeiro")
    @ApiResponse(responseCode = "422", description = "Transição inválida a partir da situação atual (PROPRIEDADE_TRANSICAO_INVALIDA)")
    public PropriedadeResponse vender(@PathVariable UUID id) {
        return paraResponse(service.vender(id));
    }

    private static PropriedadeResponse paraResponse(PropriedadeDetalhe detalhe) {
        return new PropriedadeResponse(
                detalhe.id(), detalhe.proprietarioId(), detalhe.tipo().name(), detalhe.areaPrivativa(),
                detalhe.quartos(), detalhe.vagas(), detalhe.valorReferencia().toPlainString(), detalhe.situacao().name(),
                detalhe.cep(), detalhe.logradouro(), detalhe.numero(), detalhe.complemento(), detalhe.bairro(),
                detalhe.localidade(), detalhe.uf(), detalhe.enderecoValidado(), detalhe.latitude(), detalhe.longitude(),
                detalhe.geoSituacao().name(), detalhe.criadoEm(), detalhe.alteradoEm());
    }

    private static PropriedadeResumoResponse paraResumoResponse(PropriedadeResumoView view) {
        return new PropriedadeResumoResponse(
                view.id(), view.proprietarioId(), view.tipo().name(), view.valorReferencia().toPlainString(),
                view.situacao().name(), view.logradouro(), view.bairro(), view.localidade(), view.uf(),
                view.latitude(), view.longitude(), view.geoSituacao().name(), view.criadoEm());
    }
}
