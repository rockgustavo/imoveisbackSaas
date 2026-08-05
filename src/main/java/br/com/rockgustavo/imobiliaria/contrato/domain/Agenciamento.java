package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "agenciamento")
public class Agenciamento extends Auditable {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    @Column(name = "propriedade_id", nullable = false)
    private UUID propriedadeId;

    @Column(name = "comissao_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual;

    @Column(name = "valor_pedido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPedido;

    @Column(name = "contrato_vigencia_inicio", nullable = false)
    private LocalDate contratoVigenciaInicio;

    @Column(name = "contrato_vigencia_fim", nullable = false)
    private LocalDate contratoVigenciaFim;

    @Column(name = "contrato_ativo", nullable = false)
    private boolean contratoAtivo;

    protected Agenciamento() {
    }

    Agenciamento(Contrato contrato, UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido,
                 LocalDate contratoVigenciaInicio, LocalDate contratoVigenciaFim) {
        this.id = IdGenerator.novoId();
        this.contrato = contrato;
        this.propriedadeId = Objects.requireNonNull(propriedadeId, "propriedadeId é obrigatório");
        this.comissaoPercentual = exigirPositivo(comissaoPercentual, "comissaoPercentual");
        this.valorPedido = exigirPositivo(valorPedido, "valorPedido");
        this.contratoVigenciaInicio = Objects.requireNonNull(contratoVigenciaInicio, "contratoVigenciaInicio é obrigatório");
        this.contratoVigenciaFim = Objects.requireNonNull(contratoVigenciaFim, "contratoVigenciaFim é obrigatório");
        this.contratoAtivo = false;
    }

    void ativar() {
        this.contratoAtivo = true;
    }

    void desativar() {
        this.contratoAtivo = false;
    }

    void renegociar(BigDecimal comissaoPercentual, BigDecimal valorPedido) {
        this.comissaoPercentual = exigirPositivo(comissaoPercentual, "comissaoPercentual");
        this.valorPedido = exigirPositivo(valorPedido, "valorPedido");
    }

    void exigirComissaoDentroDoTeto(BigDecimal teto) {
        if (comissaoPercentual.compareTo(teto) > 0) {
            throw new ContratoComissaoAcimaDoTetoException(comissaoPercentual, teto);
        }
    }

    private static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero");
        }
        return valor;
    }

    public UUID getId() {
        return id;
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

    public boolean isContratoAtivo() {
        return contratoAtivo;
    }
}
