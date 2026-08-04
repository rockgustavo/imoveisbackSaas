package br.com.rockgustavo.imobiliaria.orcamento.api;

import br.com.rockgustavo.imobiliaria.orcamento.application.AtualizarOrcamentoComando;
import br.com.rockgustavo.imobiliaria.orcamento.application.CriarOrcamentoComando;
import br.com.rockgustavo.imobiliaria.orcamento.application.ItemComando;
import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoDetalhe;
import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoFiltro;
import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoItemDetalhe;
import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoService;
import br.com.rockgustavo.imobiliaria.orcamento.domain.StatusOrcamento;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoResumoView;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orcamentos")
@Tag(name = "orcamento")
public class OrcamentoController {

    private final OrcamentoService service;

    public OrcamentoController(OrcamentoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cria um orçamento em RASCUNHO", description = "RN-05-01, RN-01-06")
    @ApiResponse(responseCode = "201", description = "Orçamento criado")
    @ApiResponse(responseCode = "422",
            description = "Pessoa inativa, ou propriedade inexistente no tenant (PESSOA_INATIVA_NAO_RECEBE_ORCAMENTO, ORCAMENTO_PROPRIEDADE_INDISPONIVEL)")
    public ResponseEntity<Void> criar(@Valid @RequestBody CriarOrcamentoRequest request) {
        UUID id = service.criar(new CriarOrcamentoComando(request.pessoaId(), paraItens(request.itens())));
        return ResponseEntity.created(URI.create("/api/v1/orcamentos/" + id)).build();
    }

    @GetMapping
    @Operation(summary = "Lista orçamentos do tenant", description = "filtros: pessoaId, status")
    public PageResponse<OrcamentoResumoResponse> listar(
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) StatusOrcamento status,
            @PageableDefault(size = 20) Pageable pageable) {
        PaginacaoSupport.validar(pageable);
        Page<OrcamentoResumoView> pagina = service.listar(new OrcamentoFiltro(pessoaId, status), pageable);
        return PageResponse.de(pagina.map(OrcamentoController::paraResumoResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um orçamento")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado, ou de outro tenant (ORCAMENTO_NAO_ENCONTRADO)")
    public OrcamentoResponse buscar(@PathVariable UUID id) {
        return paraResponse(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os itens de um orçamento em RASCUNHO", description = "RN-05-04")
    @ApiResponse(responseCode = "422",
            description = "Orçamento fora de RASCUNHO, ou propriedade inexistente (ORCAMENTO_NAO_EDITAVEL, ORCAMENTO_PROPRIEDADE_INDISPONIVEL)")
    @ApiResponse(responseCode = "400", description = "Propriedade repetida entre os itens (ORCAMENTO_ITEM_DUPLICADO)")
    public OrcamentoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarOrcamentoRequest request) {
        return paraResponse(service.atualizar(new AtualizarOrcamentoComando(id, paraItens(request.itens()))));
    }

    @PostMapping("/{id}/envio")
    @Operation(summary = "Envia o orçamento ao proprietário", description = "RN-05-03/08")
    @ApiResponse(responseCode = "422",
            description = "Orçamento fora de RASCUNHO, propriedade indisponível/de outro dono, ou comissão acima do teto "
                    + "(ORCAMENTO_NAO_EDITAVEL, ORCAMENTO_PROPRIEDADE_INDISPONIVEL, ORCAMENTO_COMISSAO_ACIMA_DO_TETO)")
    public OrcamentoResponse enviar(@PathVariable UUID id) {
        return paraResponse(service.enviar(id));
    }

    @PostMapping("/{id}/aceite")
    @Operation(summary = "Registra a aceitação do orçamento pelo proprietário", description = "RN-05-05, RN-05-07")
    @ApiResponse(responseCode = "422",
            description = "Orçamento não está ENVIADO com validade vigente (ORCAMENTO_EXPIRADO_OU_RECUSADO)")
    public OrcamentoResponse aceitar(@PathVariable UUID id) {
        return paraResponse(service.aceitar(id));
    }

    @PostMapping("/{id}/recusa")
    @Operation(summary = "Registra a recusa do orçamento pelo proprietário", description = "RN-05-02")
    @ApiResponse(responseCode = "422",
            description = "Orçamento não está ENVIADO com validade vigente (ORCAMENTO_EXPIRADO_OU_RECUSADO)")
    public OrcamentoResponse recusar(@PathVariable UUID id) {
        return paraResponse(service.recusar(id));
    }

    @PostMapping("/{id}/duplicacao")
    @Operation(summary = "Duplica o orçamento em uma nova versão RASCUNHO", description = "RN-05-07")
    @ApiResponse(responseCode = "201", description = "Nova versão criada, independente do status do original")
    public ResponseEntity<Void> duplicar(@PathVariable UUID id) {
        UUID novoId = service.duplicar(id);
        return ResponseEntity.created(URI.create("/api/v1/orcamentos/" + novoId)).build();
    }

    private static List<ItemComando> paraItens(List<OrcamentoItemRequest> itens) {
        return itens.stream()
                .map(item -> new ItemComando(item.propriedadeId(), item.comissaoPercentual(), item.valorPedido()))
                .toList();
    }

    private static OrcamentoResponse paraResponse(OrcamentoDetalhe detalhe) {
        List<OrcamentoItemResponse> itens = detalhe.itens().stream()
                .map(OrcamentoController::paraItemResponse)
                .toList();
        return new OrcamentoResponse(
                detalhe.id(), detalhe.pessoaId(), detalhe.status().name(), detalhe.validade().toString(),
                detalhe.origemId(), detalhe.versao(), itens, detalhe.criadoEm(), detalhe.alteradoEm());
    }

    private static OrcamentoItemResponse paraItemResponse(OrcamentoItemDetalhe item) {
        return new OrcamentoItemResponse(
                item.propriedadeId(), item.comissaoPercentual().toPlainString(), item.valorPedido().toPlainString());
    }

    private static OrcamentoResumoResponse paraResumoResponse(OrcamentoResumoView view) {
        return new OrcamentoResumoResponse(
                view.id(), view.pessoaId(), view.status().name(), view.validade().toString(), view.versao(),
                view.quantidadeItens(), view.valorTotal().toPlainString(), view.criadoEm());
    }
}
