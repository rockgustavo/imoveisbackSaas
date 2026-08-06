package br.com.rockgustavo.imobiliaria.painel.application;

import br.com.rockgustavo.imobiliaria.painel.infra.FunilClassificacaoView;
import br.com.rockgustavo.imobiliaria.painel.infra.ImoveisPorSituacaoView;
import br.com.rockgustavo.imobiliaria.painel.infra.PainelAgregadosView;

import java.util.List;

public record PainelIndicadores(
        PainelAgregadosView agregados,
        List<ImoveisPorSituacaoView> imoveisPorSituacao,
        List<FunilClassificacaoView> funil) {
}
