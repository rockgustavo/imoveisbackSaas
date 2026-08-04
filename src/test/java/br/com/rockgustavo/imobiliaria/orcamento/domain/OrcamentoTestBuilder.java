package br.com.rockgustavo.imobiliaria.orcamento.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrcamentoTestBuilder {

    private UUID pessoaId = UUID.randomUUID();
    private LocalDate validade = LocalDate.now().plusDays(15);
    private List<Orcamento.ItemProposto> itens = new ArrayList<>(List.of(itemPadrao()));

    public static OrcamentoTestBuilder umOrcamento() {
        return new OrcamentoTestBuilder();
    }

    public static Orcamento.ItemProposto itemPadrao() {
        return new Orcamento.ItemProposto(UUID.randomUUID(), new BigDecimal("6.00"), new BigDecimal("450000.00"));
    }

    public OrcamentoTestBuilder comPessoaId(UUID pessoaId) {
        this.pessoaId = pessoaId;
        return this;
    }

    public OrcamentoTestBuilder comValidade(LocalDate validade) {
        this.validade = validade;
        return this;
    }

    public OrcamentoTestBuilder comItens(List<Orcamento.ItemProposto> itens) {
        this.itens = itens;
        return this;
    }

    public Orcamento build() {
        return new Orcamento(pessoaId, validade, itens);
    }
}
