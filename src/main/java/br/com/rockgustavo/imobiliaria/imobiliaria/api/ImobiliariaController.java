package br.com.rockgustavo.imobiliaria.imobiliaria.api;

import br.com.rockgustavo.imobiliaria.imobiliaria.application.CriarImobiliariaComando;
import br.com.rockgustavo.imobiliaria.imobiliaria.application.ImobiliariaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plataforma/imobiliarias")
@Tag(name = "imobiliaria")
public class ImobiliariaController {

    private final ImobiliariaService service;

    public ImobiliariaController(ImobiliariaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Provisiona uma nova imobiliária (tenant)", description = "RN-00-06/07/08")
    @ApiResponse(responseCode = "201", description = "Imobiliária criada")
    @ApiResponse(responseCode = "400", description = "Payload inválido — CNPJ ou slug mal formado (IMOBILIARIA_CNPJ_INVALIDO, IMOBILIARIA_SLUG_INVALIDO)")
    @ApiResponse(responseCode = "409", description = "CNPJ ou slug já cadastrado (IMOBILIARIA_CNPJ_DUPLICADO, IMOBILIARIA_SLUG_DUPLICADO)")
    public ResponseEntity<Void> criar(@Valid @RequestBody CriarImobiliariaRequest request) {
        UUID id = service.criar(new CriarImobiliariaComando(request.razaoSocial(), request.cnpj(), request.slug()));
        return ResponseEntity.created(URI.create("/api/v1/plataforma/imobiliarias/" + id)).build();
    }
}
