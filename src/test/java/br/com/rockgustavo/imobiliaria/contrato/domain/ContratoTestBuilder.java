package br.com.rockgustavo.imobiliaria.contrato.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContratoTestBuilder {

    private UUID pessoaId = UUID.randomUUID();
    private UUID orcamentoOrigemId = UUID.randomUUID();
    private LocalDate vigenciaInicio = LocalDate.now();
    private LocalDate vigenciaFim = LocalDate.now().plusYears(1);
    private String regrasContratuais = "Regras contratuais padrão de teste";
    private List<Contrato.ItemParaAgenciar> itens = new ArrayList<>(List.of(itemPadrao()));

    public static ContratoTestBuilder umContrato() {
        return new ContratoTestBuilder();
    }

    public static Contrato.ItemParaAgenciar itemPadrao() {
        return new Contrato.ItemParaAgenciar(UUID.randomUUID(), new BigDecimal("6.00"), new BigDecimal("450000.00"));
    }

    public ContratoTestBuilder comPessoaId(UUID pessoaId) {
        this.pessoaId = pessoaId;
        return this;
    }

    public ContratoTestBuilder comOrcamentoOrigemId(UUID orcamentoOrigemId) {
        this.orcamentoOrigemId = orcamentoOrigemId;
        return this;
    }

    public ContratoTestBuilder comVigencia(LocalDate inicio, LocalDate fim) {
        this.vigenciaInicio = inicio;
        this.vigenciaFim = fim;
        return this;
    }

    public ContratoTestBuilder comRegrasContratuais(String regrasContratuais) {
        this.regrasContratuais = regrasContratuais;
        return this;
    }

    public ContratoTestBuilder comItens(List<Contrato.ItemParaAgenciar> itens) {
        this.itens = itens;
        return this;
    }

    public Contrato build() {
        return new Contrato(pessoaId, orcamentoOrigemId, vigenciaInicio, vigenciaFim, regrasContratuais, itens);
    }
}
