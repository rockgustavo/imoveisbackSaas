package br.com.rockgustavo.imobiliaria.orcamento.domain;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "orcamento")
public class Orcamento extends Auditable {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrcamento status;

    @Column(nullable = false)
    private LocalDate validade;

    @Column(name = "origem_id")
    private UUID origemId;

    @Column(nullable = false)
    private int versao;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrcamentoItem> itens = new ArrayList<>();

    protected Orcamento() {
    }

    public Orcamento(UUID pessoaId, LocalDate validade, List<ItemProposto> itens) {
        this.id = IdGenerator.novoId();
        this.pessoaId = Objects.requireNonNull(pessoaId, "pessoaId é obrigatório");
        this.validade = Objects.requireNonNull(validade, "validade é obrigatória");
        this.status = StatusOrcamento.RASCUNHO;
        this.versao = 1;
        substituirItens(itens);
    }

    public record ItemProposto(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
    }

    public void atualizar(List<ItemProposto> itens) {
        exigirRascunho();
        substituirItens(itens);
    }

    public void enviar() {
        exigirRascunho();
        this.status = StatusOrcamento.ENVIADO;
    }

    public void aceitar(LocalDate hojeNoFusoDoTenant) {
        exigirEnviadoDentroDaValidade(hojeNoFusoDoTenant);
        this.status = StatusOrcamento.ACEITO;
    }

    public void recusar(LocalDate hojeNoFusoDoTenant) {
        exigirEnviadoDentroDaValidade(hojeNoFusoDoTenant);
        this.status = StatusOrcamento.RECUSADO;
    }

    public void expirar() {
        if (status != StatusOrcamento.ENVIADO) {
            return;
        }
        this.status = StatusOrcamento.EXPIRADO;
    }

    public Orcamento duplicar(LocalDate novaValidade, int novaVersao, UUID raiz) {
        List<ItemProposto> copiaItens = itens.stream()
                .map(item -> new ItemProposto(item.getPropriedadeId(), item.getComissaoPercentual(), item.getValorPedido()))
                .toList();
        Orcamento copia = new Orcamento(pessoaId, novaValidade, copiaItens);
        copia.origemId = raiz;
        copia.versao = novaVersao;
        return copia;
    }

    private void exigirRascunho() {
        if (status != StatusOrcamento.RASCUNHO) {
            throw new OrcamentoNaoEditavelException(id);
        }
    }

    private void exigirEnviadoDentroDaValidade(LocalDate hojeNoFusoDoTenant) {
        if (status != StatusOrcamento.ENVIADO || validade.isBefore(hojeNoFusoDoTenant)) {
            throw new OrcamentoTransicaoInvalidaException(id);
        }
    }

    private void substituirItens(List<ItemProposto> propostos) {
        exigirSemDuplicidade(propostos);
        Set<UUID> propriedadeIdsNovos = propostos.stream().map(ItemProposto::propriedadeId).collect(Collectors.toSet());
        itens.removeIf(item -> !propriedadeIdsNovos.contains(item.getPropriedadeId()));
        Map<UUID, OrcamentoItem> existentesPorPropriedade = itens.stream()
                .collect(Collectors.toMap(OrcamentoItem::getPropriedadeId, item -> item));
        for (ItemProposto proposto : propostos) {
            OrcamentoItem existente = existentesPorPropriedade.get(proposto.propriedadeId());
            if (existente != null) {
                existente.atualizarValores(proposto.comissaoPercentual(), proposto.valorPedido());
            } else {
                itens.add(new OrcamentoItem(this, proposto.propriedadeId(), proposto.comissaoPercentual(), proposto.valorPedido()));
            }
        }
    }

    private void exigirSemDuplicidade(List<ItemProposto> propostos) {
        Set<UUID> vistos = new HashSet<>();
        for (ItemProposto proposto : propostos) {
            if (!vistos.add(proposto.propriedadeId())) {
                throw new OrcamentoItemDuplicadoException(proposto.propriedadeId());
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPessoaId() {
        return pessoaId;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public UUID getOrigemId() {
        return origemId;
    }

    public int getVersao() {
        return versao;
    }

    public List<OrcamentoItem> getItens() {
        return List.copyOf(itens);
    }
}
