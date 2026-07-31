package br.com.rockgustavo.imobiliaria.imobiliaria.domain;

import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "imobiliaria_parametro")
public class ImobiliariaParametro extends Auditable {

    private static final BigDecimal COMISSAO_TETO_PADRAO = new BigDecimal("6.00");
    private static final int VALIDADE_ORCAMENTO_PADRAO_DIAS = 15;
    private static final short GEOCODIFICACAO_TENTATIVAS_PADRAO = 5;
    private static final int CEP_CACHE_JANELA_PADRAO_DIAS = 30;
    private static final String FUSO_HORARIO_PADRAO = "America/Sao_Paulo";

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "comissao_percentual_teto", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentualTeto;

    @Column(name = "orcamento_validade_dias_padrao", nullable = false)
    private Integer orcamentoValidadeDiasPadrao;

    @Column(name = "geocodificacao_tentativas_max", nullable = false)
    private Short geocodificacaoTentativasMax;

    @Column(name = "cep_cache_janela_dias", nullable = false)
    private Integer cepCacheJanelaDias;

    @Column(name = "fuso_horario", nullable = false, length = 50)
    private String fusoHorario;

    protected ImobiliariaParametro() {
    }

    public static ImobiliariaParametro comDefaultsDePlataforma(UUID tenantId) {
        ImobiliariaParametro parametro = new ImobiliariaParametro();
        parametro.tenantId = tenantId;
        parametro.comissaoPercentualTeto = COMISSAO_TETO_PADRAO;
        parametro.orcamentoValidadeDiasPadrao = VALIDADE_ORCAMENTO_PADRAO_DIAS;
        parametro.geocodificacaoTentativasMax = GEOCODIFICACAO_TENTATIVAS_PADRAO;
        parametro.cepCacheJanelaDias = CEP_CACHE_JANELA_PADRAO_DIAS;
        parametro.fusoHorario = FUSO_HORARIO_PADRAO;
        return parametro;
    }

    public void atualizar(BigDecimal comissaoPercentualTeto, Integer orcamentoValidadeDiasPadrao,
                           Short geocodificacaoTentativasMax, Integer cepCacheJanelaDias, String fusoHorario) {
        if (comissaoPercentualTeto == null || comissaoPercentualTeto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ParametroInvalidoException("comissaoPercentualTeto deve ser maior que zero");
        }
        if (orcamentoValidadeDiasPadrao == null || orcamentoValidadeDiasPadrao <= 0) {
            throw new ParametroInvalidoException("orcamentoValidadeDiasPadrao deve ser maior que zero");
        }
        if (geocodificacaoTentativasMax == null || geocodificacaoTentativasMax <= 0) {
            throw new ParametroInvalidoException("geocodificacaoTentativasMax deve ser maior que zero");
        }
        if (cepCacheJanelaDias == null || cepCacheJanelaDias <= 0) {
            throw new ParametroInvalidoException("cepCacheJanelaDias deve ser maior que zero");
        }
        if (fusoHorario == null || fusoHorario.isBlank()) {
            throw new ParametroInvalidoException("fusoHorario é obrigatório");
        }
        this.comissaoPercentualTeto = comissaoPercentualTeto;
        this.orcamentoValidadeDiasPadrao = orcamentoValidadeDiasPadrao;
        this.geocodificacaoTentativasMax = geocodificacaoTentativasMax;
        this.cepCacheJanelaDias = cepCacheJanelaDias;
        this.fusoHorario = fusoHorario;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public BigDecimal getComissaoPercentualTeto() {
        return comissaoPercentualTeto;
    }

    public Integer getOrcamentoValidadeDiasPadrao() {
        return orcamentoValidadeDiasPadrao;
    }

    public Short getGeocodificacaoTentativasMax() {
        return geocodificacaoTentativasMax;
    }

    public Integer getCepCacheJanelaDias() {
        return cepCacheJanelaDias;
    }

    public String getFusoHorario() {
        return fusoHorario;
    }
}
