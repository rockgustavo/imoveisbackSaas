package br.com.rockgustavo.imobiliaria.propriedade.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class PropriedadeTestBuilder {

    private UUID proprietarioId = UUID.randomUUID();
    private TipoPropriedade tipo = TipoPropriedade.APARTAMENTO;
    private BigDecimal areaPrivativa = new BigDecimal("85.50");
    private Short quartos = 3;
    private Short vagas = 1;
    private BigDecimal valorReferencia = new BigDecimal("450000.00");
    private Endereco endereco = enderecoPadrao();

    public static PropriedadeTestBuilder umaPropriedade() {
        return new PropriedadeTestBuilder();
    }

    public static Endereco enderecoPadrao() {
        return new Endereco("01310100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", true);
    }

    public PropriedadeTestBuilder comProprietarioId(UUID proprietarioId) {
        this.proprietarioId = proprietarioId;
        return this;
    }

    public PropriedadeTestBuilder comValorReferencia(BigDecimal valorReferencia) {
        this.valorReferencia = valorReferencia;
        return this;
    }

    public PropriedadeTestBuilder comEndereco(Endereco endereco) {
        this.endereco = endereco;
        return this;
    }

    public Propriedade build() {
        return new Propriedade(proprietarioId, tipo, areaPrivativa, quartos, vagas, valorReferencia, endereco);
    }
}
