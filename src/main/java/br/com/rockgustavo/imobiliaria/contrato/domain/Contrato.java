package br.com.rockgustavo.imobiliaria.contrato.domain;

import br.com.rockgustavo.imobiliaria.shared.IdGenerator;
import br.com.rockgustavo.imobiliaria.shared.audit.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contrato")
public class Contrato extends Auditable {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "orcamento_origem_id", nullable = false)
    private UUID orcamentoOrigemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContrato status;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim", nullable = false)
    private LocalDate vigenciaFim;

    @Column(name = "regras_contratuais", nullable = false)
    private String regrasContratuais;

    @Column(name = "justificativa_encerramento")
    private String justificativaEncerramento;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Agenciamento> agenciamentos = new ArrayList<>();

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aditivo> aditivos = new ArrayList<>();

    protected Contrato() {
    }

    public Contrato(UUID pessoaId, UUID orcamentoOrigemId, LocalDate vigenciaInicio, LocalDate vigenciaFim,
                     String regrasContratuais, List<ItemParaAgenciar> itens) {
        this.id = IdGenerator.novoId();
        this.pessoaId = Objects.requireNonNull(pessoaId, "pessoaId é obrigatório");
        this.orcamentoOrigemId = Objects.requireNonNull(orcamentoOrigemId, "orcamentoOrigemId é obrigatório");
        this.vigenciaInicio = Objects.requireNonNull(vigenciaInicio, "vigenciaInicio é obrigatório");
        this.vigenciaFim = exigirFimPosteriorAoInicio(vigenciaInicio, vigenciaFim);
        this.regrasContratuais = exigirNaoBranco(regrasContratuais, "regrasContratuais");
        this.status = StatusContrato.RASCUNHO;
        exigirAoMenosUmItem(itens);
        for (ItemParaAgenciar item : itens) {
            agenciamentos.add(new Agenciamento(this, item.propriedadeId(), item.comissaoPercentual(), item.valorPedido(),
                    vigenciaInicio, vigenciaFim));
        }
    }

    public record ItemParaAgenciar(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
    }

    public void ativar(BigDecimal comissaoTeto) {
        exigirTransicao(StatusContrato.ATIVO);
        for (Agenciamento agenciamento : agenciamentos) {
            agenciamento.exigirComissaoDentroDoTeto(comissaoTeto);
        }
        this.status = StatusContrato.ATIVO;
        agenciamentos.forEach(Agenciamento::ativar);
    }

    public void encerrar(String justificativa) {
        exigirTransicao(StatusContrato.ENCERRADO);
        this.justificativaEncerramento = exigirNaoBranco(justificativa, "justificativa");
        this.status = StatusContrato.ENCERRADO;
        agenciamentos.forEach(Agenciamento::desativar);
    }

    public void cancelar() {
        exigirTransicao(StatusContrato.CANCELADO);
        boolean liberaAgenciamentos = status == StatusContrato.ATIVO;
        this.status = StatusContrato.CANCELADO;
        if (liberaAgenciamentos) {
            agenciamentos.forEach(Agenciamento::desativar);
        }
    }

    public void expirar() {
        if (status != StatusContrato.ATIVO) {
            return;
        }
        this.status = StatusContrato.EXPIRADO;
        agenciamentos.forEach(Agenciamento::desativar);
    }

    public UUID incluirPropriedade(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido,
                                    BigDecimal comissaoTeto, String justificativa) {
        exigirAtivo();
        Agenciamento existente = buscarAgenciamentoAtivo(propriedadeId);
        if (existente != null) {
            existente.renegociar(comissaoPercentual, valorPedido);
            existente.exigirComissaoDentroDoTeto(comissaoTeto);
            aditivos.add(new Aditivo(this, propriedadeId, TipoAditivo.EXCLUSAO, justificativa));
            aditivos.add(new Aditivo(this, propriedadeId, TipoAditivo.INCLUSAO, justificativa));
            return existente.getId();
        }
        Agenciamento novo = new Agenciamento(this, propriedadeId, comissaoPercentual, valorPedido, vigenciaInicio, vigenciaFim);
        novo.exigirComissaoDentroDoTeto(comissaoTeto);
        novo.ativar();
        agenciamentos.add(novo);
        aditivos.add(new Aditivo(this, propriedadeId, TipoAditivo.INCLUSAO, justificativa));
        return novo.getId();
    }

    public void excluirPropriedade(UUID propriedadeId, String justificativa) {
        exigirAtivo();
        Agenciamento alvo = buscarAgenciamentoAtivo(propriedadeId);
        if (alvo == null) {
            throw new ContratoAgenciamentoNaoEncontradoException(id, propriedadeId);
        }
        agenciamentos.remove(alvo);
        aditivos.add(new Aditivo(this, propriedadeId, TipoAditivo.EXCLUSAO, justificativa));
    }

    public boolean possuiAgenciamentoAtivoPara(UUID propriedadeId) {
        return buscarAgenciamentoAtivo(propriedadeId) != null;
    }

    private Agenciamento buscarAgenciamentoAtivo(UUID propriedadeId) {
        return agenciamentos.stream()
                .filter(a -> a.getPropriedadeId().equals(propriedadeId) && a.isContratoAtivo())
                .findFirst()
                .orElse(null);
    }

    private void exigirAtivo() {
        if (status != StatusContrato.ATIVO) {
            throw new ContratoTransicaoInvalidaException(id, status);
        }
    }

    private void exigirTransicao(StatusContrato destino) {
        if (!status.podeTransicionarPara(destino)) {
            throw new ContratoTransicaoInvalidaException(id, status);
        }
    }

    private static LocalDate exigirFimPosteriorAoInicio(LocalDate inicio, LocalDate fim) {
        if (fim == null || !fim.isAfter(inicio)) {
            throw new ContratoVigenciaInvalidaException(inicio, fim);
        }
        return fim;
    }

    private static void exigirAoMenosUmItem(List<ItemParaAgenciar> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("contrato exige ao menos uma propriedade");
        }
    }

    private static String exigirNaoBranco(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPessoaId() {
        return pessoaId;
    }

    public UUID getOrcamentoOrigemId() {
        return orcamentoOrigemId;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public String getRegrasContratuais() {
        return regrasContratuais;
    }

    public String getJustificativaEncerramento() {
        return justificativaEncerramento;
    }

    public List<Agenciamento> getAgenciamentos() {
        return List.copyOf(agenciamentos);
    }

    public List<Aditivo> getAditivos() {
        return List.copyOf(aditivos);
    }

    public List<UUID> getPropriedadeIds() {
        return agenciamentos.stream().map(Agenciamento::getPropriedadeId).toList();
    }
}
