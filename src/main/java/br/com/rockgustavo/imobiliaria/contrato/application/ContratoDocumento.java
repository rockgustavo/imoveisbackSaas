package br.com.rockgustavo.imobiliaria.contrato.application;

import java.util.List;

public record ContratoDocumento(
        String numeroContrato,
        String statusContrato,
        String razaoSocialContratada,
        String cnpjContratada,
        String nomeContratante,
        String documentoContratante,
        List<ItemObjeto> itens,
        String vigenciaInicio,
        String vigenciaFim,
        String regrasContratuais,
        List<ItemAditivo> aditivos,
        String emitidoEm,
        String notaHistorica) {

    public record ItemObjeto(String tipoImovel, String endereco, String comissaoPercentual, String valorPedido) {
    }

    public record ItemAditivo(String data, String tipo, String justificativa) {
    }
}
