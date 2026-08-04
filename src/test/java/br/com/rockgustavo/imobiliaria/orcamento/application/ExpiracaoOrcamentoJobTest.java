package br.com.rockgustavo.imobiliaria.orcamento.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoEnviadoView;
import br.com.rockgustavo.imobiliaria.orcamento.infra.OrcamentoQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiracaoOrcamentoJobTest {

    @Mock
    OrcamentoQueryRepository queryRepository;

    @Mock
    OrcamentoService orcamentoService;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    private ExpiracaoOrcamentoJob novoJob() {
        return new ExpiracaoOrcamentoJob(queryRepository, orcamentoService, imobiliariaFacade);
    }

    @Test
    @DisplayName("RN-05-03: expira apenas candidatos com validade anterior a hoje no fuso do tenant")
    void expiraApenasCandidatosVencidos() {
        UUID tenantId = UUID.randomUUID();
        UUID vencidoId = UUID.randomUUID();
        UUID vigenteId = UUID.randomUUID();
        when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        when(queryRepository.buscarEnviadosDeTodosOsTenants()).thenReturn(List.of(
                new OrcamentoEnviadoView(vencidoId, tenantId, hoje.minusDays(1)),
                new OrcamentoEnviadoView(vigenteId, tenantId, hoje.plusDays(1))));

        novoJob().executar();

        verify(orcamentoService).expirarSeAplicavel(eq(vencidoId), eq(hoje));
        verify(orcamentoService, never()).expirarSeAplicavel(eq(vigenteId), any());
    }

    @Test
    @DisplayName("resolve o fuso de cada tenant só uma vez por execução")
    void resolveFusoUmaVezPorTenant() {
        UUID tenantId = UUID.randomUUID();
        when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
        LocalDate ontem = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        when(queryRepository.buscarEnviadosDeTodosOsTenants()).thenReturn(List.of(
                new OrcamentoEnviadoView(UUID.randomUUID(), tenantId, ontem),
                new OrcamentoEnviadoView(UUID.randomUUID(), tenantId, ontem)));

        novoJob().executar();

        verify(imobiliariaFacade, times(1)).fusoHorario(tenantId);
    }

    @Test
    @DisplayName("sem candidatos vencidos, não chama o serviço de expiração")
    void semCandidatosVencidosNaoChamaServico() {
        when(queryRepository.buscarEnviadosDeTodosOsTenants()).thenReturn(List.of());

        novoJob().executar();

        verify(orcamentoService, never()).expirarSeAplicavel(any(), any());
    }
}
