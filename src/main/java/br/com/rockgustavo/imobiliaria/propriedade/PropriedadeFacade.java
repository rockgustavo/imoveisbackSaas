package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.propriedade.application.PropriedadeService;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeEnderecoView;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeQueryRepository;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PropriedadeFacade {

    private final PropriedadeRepository propriedadeRepository;
    private final PropriedadeQueryRepository propriedadeQueryRepository;
    private final PropriedadeService propriedadeService;

    public PropriedadeFacade(PropriedadeRepository propriedadeRepository,
                              PropriedadeQueryRepository propriedadeQueryRepository,
                              PropriedadeService propriedadeService) {
        this.propriedadeRepository = propriedadeRepository;
        this.propriedadeQueryRepository = propriedadeQueryRepository;
        this.propriedadeService = propriedadeService;
    }

    public boolean existe(UUID propriedadeId) {
        return propriedadeRepository.existsById(propriedadeId);
    }

    public boolean disponivelParaAgenciamentoPor(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioIdAndSituacao(
                propriedadeId, proprietarioId, SituacaoPropriedade.DISPONIVEL);
    }

    public boolean pertenceAoProprietario(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioId(propriedadeId, proprietarioId);
    }

    public boolean semNegociacaoEmAndamento(UUID propriedadeId) {
        return propriedadeRepository.existsByIdAndSituacaoNotIn(
                propriedadeId, List.of(SituacaoPropriedade.RESERVADA, SituacaoPropriedade.VENDIDA));
    }

    public boolean podeSerAgenciadaPor(UUID propriedadeId, UUID proprietarioId) {
        return propriedadeRepository.existsByIdAndProprietarioIdAndSituacaoNotIn(propriedadeId, proprietarioId,
                List.of(SituacaoPropriedade.RESERVADA, SituacaoPropriedade.VENDIDA, SituacaoPropriedade.RETIRADA));
    }

    public void agenciar(UUID propriedadeId) {
        propriedadeService.marcarAgenciada(propriedadeId);
    }

    public void liberarAgenciamento(UUID propriedadeId) {
        propriedadeService.liberarAgenciamento(propriedadeId);
    }

    public Map<UUID, QualificacaoImovel> qualificacoes(UUID tenantId, Collection<UUID> propriedadeIds) {
        return propriedadeQueryRepository.buscarEnderecos(tenantId, propriedadeIds).stream()
                .collect(Collectors.toMap(PropriedadeEnderecoView::id, PropriedadeFacade::paraQualificacao));
    }

    private static QualificacaoImovel paraQualificacao(PropriedadeEnderecoView view) {
        return new QualificacaoImovel(view.tipo().name(), view.logradouro(), view.numero(), view.complemento(),
                view.bairro(), view.localidade(), view.uf());
    }

    public record QualificacaoImovel(String tipo, String logradouro, String numero, String complemento,
                                      String bairro, String localidade, String uf) {
    }
}
