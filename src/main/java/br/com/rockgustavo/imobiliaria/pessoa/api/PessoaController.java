package br.com.rockgustavo.imobiliaria.pessoa.api;

import br.com.rockgustavo.imobiliaria.pessoa.application.AtribuirPapelComando;
import br.com.rockgustavo.imobiliaria.pessoa.application.AtualizarPessoaComando;
import br.com.rockgustavo.imobiliaria.pessoa.application.CriarPessoaComando;
import br.com.rockgustavo.imobiliaria.pessoa.application.PessoaDetalhe;
import br.com.rockgustavo.imobiliaria.pessoa.application.PessoaFiltro;
import br.com.rockgustavo.imobiliaria.pessoa.application.PessoaService;
import br.com.rockgustavo.imobiliaria.pessoa.domain.ClassificacaoComercial;
import br.com.rockgustavo.imobiliaria.pessoa.domain.Papel;
import br.com.rockgustavo.imobiliaria.pessoa.infra.PessoaResumoView;
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

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pessoas")
@Tag(name = "pessoa")
public class PessoaController {

    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma pessoa", description = "RN-01-01/02")
    @ApiResponse(responseCode = "201", description = "Pessoa criada")
    @ApiResponse(responseCode = "400", description = "Documento inválido (PESSOA_DOCUMENTO_INVALIDO)")
    @ApiResponse(responseCode = "401", description = "Sem token")
    @ApiResponse(responseCode = "403", description = "Sem papel ADMINISTRADOR")
    @ApiResponse(responseCode = "409", description = "Documento ou e-mail já cadastrado no tenant (PESSOA_DOCUMENTO_DUPLICADO, PESSOA_EMAIL_DUPLICADO)")
    public ResponseEntity<Void> criar(@Valid @RequestBody CriarPessoaRequest request) {
        UUID id = service.criar(new CriarPessoaComando(
                request.tipoDocumento(), request.documento(), request.nome(), request.email()));
        return ResponseEntity.created(URI.create("/api/v1/pessoas/" + id)).build();
    }

    @GetMapping
    @Operation(summary = "Lista pessoas do tenant", description = "RN-01-08/09")
    public PageResponse<PessoaResponse> listar(
            @RequestParam(required = false) String documento,
            @RequestParam(required = false) Papel papel,
            @RequestParam(required = false) ClassificacaoComercial classificacao,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20) Pageable pageable) {
        PaginacaoSupport.validar(pageable);
        PessoaFiltro filtro = new PessoaFiltro(documento, papel, classificacao, ativo);
        Page<PessoaResumoView> pagina = service.listar(filtro, pageable);
        return PageResponse.de(pagina.map(PessoaController::paraResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma pessoa")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada, ou de outro tenant (PESSOA_NAO_ENCONTRADA)")
    public PessoaResponse buscar(@PathVariable UUID id) {
        return paraResponse(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados cadastrais da pessoa")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado no tenant (PESSOA_EMAIL_DUPLICADO)")
    public PessoaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarPessoaRequest request) {
        return paraResponse(service.atualizar(new AtualizarPessoaComando(id, request.nome(), request.email())));
    }

    @PostMapping("/{id}/papeis")
    @Operation(summary = "Atribui papel a uma pessoa", description = "RN-01-03/04/10")
    @ApiResponse(responseCode = "409", description = "Papel já atribuído ou e-mail duplicado (PESSOA_PAPEL_JA_ATRIBUIDO, PESSOA_EMAIL_DUPLICADO)")
    @ApiResponse(responseCode = "422", description = "E-mail obrigatório para USUARIO/ADMINISTRADOR (PESSOA_EMAIL_OBRIGATORIO)")
    @ApiResponse(responseCode = "502", description = "Falha ao provisionar credencial no Keycloak (CREDENCIAL_PROVISIONAMENTO_FALHOU)")
    public PessoaResponse atribuirPapel(@PathVariable UUID id, @Valid @RequestBody AtribuirPapelRequest request) {
        AtribuirPapelComando comando = new AtribuirPapelComando(id, request.papel(), request.email());
        return paraResponse(service.atribuirPapel(comando));
    }

    @DeleteMapping("/{id}/papeis/{papel}")
    @Operation(summary = "Remove papel de uma pessoa", description = "RN-01-07")
    @ApiResponse(responseCode = "204", description = "Papel removido")
    @ApiResponse(responseCode = "422", description = "Último ADMINISTRADOR ativo não pode ser removido (PESSOA_ULTIMO_ADMINISTRADOR)")
    public ResponseEntity<Void> removerPapel(@PathVariable UUID id, @PathVariable Papel papel) {
        service.removerPapel(id, papel);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inativacao")
    @Operation(summary = "Inativa uma pessoa", description = "RN-01-05/06, RN-02-04")
    @ApiResponse(responseCode = "204", description = "Pessoa inativada")
    @ApiResponse(responseCode = "422", description = "Último ADMINISTRADOR ativo não pode ser inativado (PESSOA_ULTIMO_ADMINISTRADOR)")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }

    private static PessoaResponse paraResponse(PessoaDetalhe detalhe) {
        return new PessoaResponse(
                detalhe.id(), detalhe.tipoDocumento().name(), detalhe.documento(), detalhe.nome(), detalhe.email(),
                detalhe.ativo(), detalhe.papeis().stream().map(Enum::name).toList(),
                ClassificacaoComercial.LEAD.name(), detalhe.criadoEm(), detalhe.alteradoEm());
    }

    private static PessoaResponse paraResponse(PessoaResumoView view) {
        return new PessoaResponse(
                view.id(), view.tipoDocumento().name(), view.documento(), view.nome(), view.email(),
                view.ativo(), view.papeis().stream().map(Enum::name).toList(),
                view.classificacao().name(), view.criadoEm(), view.alteradoEm());
    }
}
