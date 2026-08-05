package br.com.rockgustavo.imobiliaria.orcamento;

import br.com.rockgustavo.imobiliaria.orcamento.application.OrcamentoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class OrcamentoFacade {

    private final OrcamentoService orcamentoService;

    public OrcamentoFacade(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    public OrcamentoAceitoDetalhe buscarAceito(UUID orcamentoId) {
        return orcamentoService.buscarAceito(orcamentoId);
    }

    public record OrcamentoAceitoDetalhe(UUID pessoaId, List<ItemAceito> itens) {
    }

    public record ItemAceito(UUID propriedadeId, BigDecimal comissaoPercentual, BigDecimal valorPedido) {
    }
}
