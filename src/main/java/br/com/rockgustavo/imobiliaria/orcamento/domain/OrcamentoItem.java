package br.com.rockgustavo.imobiliaria.orcamento.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orcamento_item")
public class OrcamentoItem {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @Column(name = "propriedade_id", nullable = false)
    private UUID propriedadeId;

    @Column(name = "comissao_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual;

    @Column(name = "valor_pedido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPedido;

    protected OrcamentoItem() {
    }

    OrcamentoItem(Orcamento orcamento, UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
        this.id = IdGenerator.novoId();
        this.orcamento = orcamento;
        this.propriedadeId = Objects.requireNonNull(propriedadeId, "propriedadeId é obrigatório");
        this.comissaoPercentual = exigirPositivo(comissaoPercentual, "comissaoPercentual");
        this.valorPedido = exigirPositivo(valorPedido, "valorPedido");
    }

    void atualizarValores(BigDecimal comissaoPercentual, BigDecimal valorPedido) {
        this.comissaoPercentual = exigirPositivo(comissaoPercentual, "comissaoPercentual");
        this.valorPedido = exigirPositivo(valorPedido, "valorPedido");
    }

    private static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero");
        }
        return valor;
    }

    public UUID getPropriedadeId() {
        return propriedadeId;
    }

    public BigDecimal getComissaoPercentual() {
        return comissaoPercentual;
    }

    public BigDecimal getValorPedido() {
        return valorPedido;
    }
}
