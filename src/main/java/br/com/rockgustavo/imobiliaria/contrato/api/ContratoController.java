package br.com.rockgustavo.imobiliaria.contrato.api;

import br.com.rockgustavo.imobiliaria.contrato.application.AditivoComando;
import br.com.rockgustavo.imobiliaria.contrato.application.AditivoDetalhe;
import br.com.rockgustavo.imobiliaria.contrato.application.AgenciamentoDetalhe;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoDetalhe;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoFiltro;
import br.com.rockgustavo.imobiliaria.contrato.application.ContratoService;
import br.com.rockgustavo.imobiliaria.contrato.application.CriarContratoComando;
import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoResumoView;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contratos")
@Tag(name = "contrato")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um contrato em RASCUNHO a partir de um orçamento aceito", description = "RN-06-01/02, RN-05-05")
    @ApiResponse(responseCode = "201", description = "Contrato criado")
    @ApiResponse(responseCode = "422",
            description = "Orçamento não aceito, ou propriedade do orçamento não pertence mais ao proprietário "
                    + "(ORCAMENTO_NAO_ACEITO, PROPRIEDADE_PROPRIETARIO_DIVERGENTE)")
    @ApiResponse(responseCode = "409", description = "Orçamento já originou outro contrato (ORCAMENTO_JA_ORIGINOU_CONTRATO)")
    @ApiResponse(responseCode = "400", description = "Vigência inválida (CONTRATO_VIGENCIA_INVALIDA)")
    public ResponseEntity<Void> criar(@Valid @RequestBody CriarContratoRequest request) {
        UUID id = service.criar(new CriarContratoComando(request.orcamentoId(), request.vigenciaInicio(),
                request.vigenciaFim(), request.regrasContratuais()));
        return ResponseEntity.created(URI.create("/api/v1/contratos/" + id)).build();
    }

    @GetMapping
    @Operation(summary = "Lista contratos do tenant", description = "filtros: pessoaId, status, vencendoEmDias")
    public PageResponse<ContratoResumoResponse> listar(
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) StatusContrato status,
            @RequestParam(required = false) Integer vencendoEmDias,
            @PageableDefault(size = 20) Pageable pageable) {
        PaginacaoSupport.validar(pageable);
        Page<ContratoResumoView> pagina = service.listar(new ContratoFiltro(pessoaId, status, vencendoEmDias), pageable);
        return PageResponse.de(pagina.map(ContratoController::paraResumoResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um contrato, com agenciamentos e aditivos embutidos")
    @ApiResponse(responseCode = "404", description = "Contrato não encontrado, ou de outro tenant (CONTRATO_NAO_ENCONTRADO)")
    public ContratoResponse buscar(@PathVariable UUID id) {
        return paraResponse(service.buscarPorId(id));
    }

    @PostMapping("/{id}/ativacao")
    @Operation(summary = "Ativa o contrato, movendo as propriedades para AGENCIADA", description = "RN-06-05/06/07")
    @ApiResponse(responseCode = "422",
            description = "Propriedade indisponível ou comissão acima do teto "
                    + "(CONTRATO_PROPRIEDADE_INDISPONIVEL, CONTRATO_COMISSAO_ACIMA_DO_TETO, CONTRATO_TRANSICAO_INVALIDA)")
    @ApiResponse(responseCode = "409", description = "Vigência sobreposta a outro contrato ativo (CONTRATO_VIGENCIA_SOBREPOSTA)")
    public ContratoResponse ativar(@PathVariable UUID id) {
        return paraResponse(service.ativar(id));
    }

    @PostMapping("/{id}/encerramento")
    @Operation(summary = "Encerra antecipadamente um contrato ATIVO", description = "RN-06-04")
    @ApiResponse(responseCode = "422", description = "Contrato não está ATIVO (CONTRATO_TRANSICAO_INVALIDA)")
    public ContratoResponse encerrar(@PathVariable UUID id, @Valid @RequestBody EncerramentoRequest request) {
        return paraResponse(service.encerrar(id, request.justificativa()));
    }

    @PostMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela um contrato RASCUNHO ou ATIVO sem negociação em andamento", description = "RN-06-10")
    @ApiResponse(responseCode = "422",
            description = "Propriedade RESERVADA ou VENDIDA impede o cancelamento (CONTRATO_CANCELAMENTO_INVIAVEL)")
    public ContratoResponse cancelar(@PathVariable UUID id) {
        return paraResponse(service.cancelar(id));
    }

    @PostMapping("/{id}/aditivos")
    @Operation(summary = "Registra inclusão, exclusão ou renegociação de propriedade em contrato ATIVO",
            description = "RN-06-08/13")
    @ApiResponse(responseCode = "422",
            description = "Propriedade indisponível, comissão acima do teto, contrato fora de ATIVO, ou propriedade "
                    + "não agenciada neste contrato (CONTRATO_PROPRIEDADE_INDISPONIVEL, CONTRATO_COMISSAO_ACIMA_DO_TETO, "
                    + "CONTRATO_TRANSICAO_INVALIDA, CONTRATO_AGENCIAMENTO_NAO_ENCONTRADO)")
    @ApiResponse(responseCode = "409", description = "Vigência sobreposta a outro contrato ativo (CONTRATO_VIGENCIA_SOBREPOSTA)")
    public ContratoResponse registrarAditivo(@PathVariable UUID id, @Valid @RequestBody AditivoRequest request) {
        AditivoComando comando = new AditivoComando(id, request.tipo(), request.propriedadeId(), request.justificativa(),
                request.comissaoPercentual(), request.valorPedido());
        return paraResponse(service.registrarAditivo(comando));
    }

    private static ContratoResponse paraResponse(ContratoDetalhe detalhe) {
        List<AgenciamentoResponse> agenciamentos = detalhe.agenciamentos().stream()
                .map(ContratoController::paraAgenciamentoResponse)
                .toList();
        List<AditivoResponse> aditivos = detalhe.aditivos().stream()
                .map(ContratoController::paraAditivoResponse)
                .toList();
        return new ContratoResponse(
                detalhe.id(), detalhe.pessoaId(), detalhe.orcamentoOrigemId(), detalhe.status().name(),
                detalhe.vigenciaInicio().toString(), detalhe.vigenciaFim().toString(), detalhe.regrasContratuais(),
                detalhe.justificativaEncerramento(), agenciamentos, aditivos, detalhe.criadoEm(), detalhe.alteradoEm());
    }

    private static AgenciamentoResponse paraAgenciamentoResponse(AgenciamentoDetalhe agenciamento) {
        return new AgenciamentoResponse(
                agenciamento.id(), agenciamento.propriedadeId(), agenciamento.comissaoPercentual().toPlainString(),
                agenciamento.valorPedido().toPlainString(), agenciamento.contratoAtivo());
    }

    private static AditivoResponse paraAditivoResponse(AditivoDetalhe aditivo) {
        return new AditivoResponse(
                aditivo.propriedadeId(), aditivo.tipo().name(), aditivo.justificativa(), aditivo.data().toString());
    }

    private static ContratoResumoResponse paraResumoResponse(ContratoResumoView view) {
        return new ContratoResumoResponse(
                view.id(), view.pessoaId(), view.status().name(), view.vigenciaInicio().toString(),
                view.vigenciaFim().toString(), view.quantidadeAgenciamentos(), view.valorTotal().toPlainString(),
                view.criadoEm());
    }
}
