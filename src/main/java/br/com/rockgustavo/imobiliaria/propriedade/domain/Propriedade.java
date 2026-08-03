package br.com.rockgustavo.imobiliaria.propriedade.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "propriedade")
@DynamicUpdate
public class Propriedade extends Auditable {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPropriedade tipo;

    @Column(name = "area_privativa", precision = 10, scale = 2)
    private BigDecimal areaPrivativa;

    private Short quartos;

    private Short vagas;

    @Column(name = "valor_referencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoPropriedade situacao;

    @Embedded
    private Endereco endereco;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "geo_situacao", nullable = false, length = 20)
    private GeoSituacao geoSituacao;

    @Column(name = "geo_tentativas", nullable = false)
    private short geoTentativas;

    protected Propriedade() {
    }

    public Propriedade(UUID proprietarioId, TipoPropriedade tipo, BigDecimal areaPrivativa, Short quartos,
                        Short vagas, BigDecimal valorReferencia, Endereco endereco) {
        this.id = IdGenerator.novoId();
        this.proprietarioId = Objects.requireNonNull(proprietarioId, "proprietarioId é obrigatório");
        this.tipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.areaPrivativa = areaPrivativa;
        this.quartos = quartos;
        this.vagas = vagas;
        this.valorReferencia = exigirPositivo(valorReferencia, "valorReferencia");
        this.endereco = Objects.requireNonNull(endereco, "endereco é obrigatório");
        this.situacao = SituacaoPropriedade.DISPONIVEL;
        this.geoSituacao = GeoSituacao.PENDENTE;
        this.geoTentativas = 0;
    }

    private static BigDecimal exigirPositivo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " deve ser maior que zero");
        }
        return valor;
    }

    public boolean atualizar(TipoPropriedade tipo, BigDecimal areaPrivativa, Short quartos, Short vagas,
                              BigDecimal valorReferencia, Endereco endereco) {
        this.tipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.areaPrivativa = areaPrivativa;
        this.quartos = quartos;
        this.vagas = vagas;
        this.valorReferencia = exigirPositivo(valorReferencia, "valorReferencia");
        boolean enderecoMudou = !this.endereco.equals(Objects.requireNonNull(endereco, "endereco é obrigatório"));
        this.endereco = endereco;
        if (enderecoMudou) {
            reiniciarGeolocalizacao();
        }
        return enderecoMudou;
    }

    private void reiniciarGeolocalizacao() {
        this.geoSituacao = GeoSituacao.PENDENTE;
        this.geoTentativas = 0;
        this.latitude = null;
        this.longitude = null;
    }

    public void concluirGeolocalizacao(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = Objects.requireNonNull(latitude, "latitude é obrigatória");
        this.longitude = Objects.requireNonNull(longitude, "longitude é obrigatória");
        this.geoSituacao = GeoSituacao.CONCLUIDA;
    }

    public void registrarTentativaFalhaGeo(short tentativasMax) {
        this.geoTentativas++;
        if (this.geoTentativas >= tentativasMax) {
            this.geoSituacao = GeoSituacao.MANUAL;
        }
    }

    public void trocarProprietario(UUID novoProprietarioId) {
        if (situacao != SituacaoPropriedade.DISPONIVEL && situacao != SituacaoPropriedade.RETIRADA) {
            throw new PropriedadeComAgenciamentoVigenteException(id);
        }
        this.proprietarioId = Objects.requireNonNull(novoProprietarioId, "proprietarioId é obrigatório");
    }

    public void retirar() {
        transicionarPara(SituacaoPropriedade.RETIRADA);
    }

    public void reservar() {
        transicionarPara(SituacaoPropriedade.RESERVADA);
    }

    public void desfazerReserva() {
        if (situacao != SituacaoPropriedade.RESERVADA) {
            throw new PropriedadeTransicaoInvalidaException(situacao, SituacaoPropriedade.AGENCIADA);
        }
        transicionarPara(SituacaoPropriedade.AGENCIADA);
    }

    public void vender() {
        transicionarPara(SituacaoPropriedade.VENDIDA);
    }

    public void transicionarPara(SituacaoPropriedade novaSituacao) {
        if (!situacao.podeTransicionarPara(novaSituacao)) {
            throw new PropriedadeTransicaoInvalidaException(situacao, novaSituacao);
        }
        this.situacao = novaSituacao;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProprietarioId() {
        return proprietarioId;
    }

    public TipoPropriedade getTipo() {
        return tipo;
    }

    public BigDecimal getAreaPrivativa() {
        return areaPrivativa;
    }

    public Short getQuartos() {
        return quartos;
    }

    public Short getVagas() {
        return vagas;
    }

    public BigDecimal getValorReferencia() {
        return valorReferencia;
    }

    public SituacaoPropriedade getSituacao() {
        return situacao;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public GeoSituacao getGeoSituacao() {
        return geoSituacao;
    }

    public short getGeoTentativas() {
        return geoTentativas;
    }
}
