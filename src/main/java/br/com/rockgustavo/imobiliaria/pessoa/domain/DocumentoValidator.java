package br.com.rockgustavo.imobiliaria.pessoa.domain;

import br.com.rockgustavo.imobiliaria.shared.validation.CnpjValidator;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfValidator;

public final class DocumentoValidator {

    private DocumentoValidator() {
    }

    public static String validar(TipoDocumento tipoDocumento, String documento) {
        String digitos = documento == null ? null : documento.replaceAll("\\D", "");
        boolean valido = switch (tipoDocumento) {
            case CPF -> CpfValidator.valido(digitos);
            case CNPJ -> CnpjValidator.valido(digitos);
        };
        if (!valido) {
            throw new DocumentoInvalidoException(tipoDocumento, documento);
        }
        return digitos;
    }
}
