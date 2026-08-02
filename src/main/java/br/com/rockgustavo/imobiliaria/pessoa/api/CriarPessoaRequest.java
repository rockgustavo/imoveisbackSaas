package br.com.rockgustavo.imobiliaria.pessoa.api;

import br.com.rockgustavo.imobiliaria.pessoa.domain.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarPessoaRequest(
        @NotNull TipoDocumento tipoDocumento,
        @NotBlank @Schema(example = "52998224725") String documento,
        @NotBlank String nome,
        @Email String email) {
}
