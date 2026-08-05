package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoAtivoView;
import br.com.rockgustavo.imobiliaria.contrato.infra.ContratoQueryRepository;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
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
class ContratoExpiracaoJobTest {

    @Mock
    ContratoQueryRepository queryRepository;

    @Mock
    ContratoService contratoService;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    private ContratoExpiracaoJob novoJob() {
        return new ContratoExpiracaoJob(queryRepository, contratoService, imobiliariaFacade);
    }

    @Test
    @DisplayName("RN-06-09: expira apenas candidatos com vigência encerrada antes de hoje no fuso do tenant")
    void expiraApenasCandidatosVencidos() {
        UUID tenantId = UUID.randomUUID();
        UUID vencidoId = UUID.randomUUID();
        UUID vigenteId = UUID.randomUUID();
        when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        when(queryRepository.buscarAtivosDeTodosOsTenants()).thenReturn(List.of(
                new ContratoAtivoView(vencidoId, tenantId, hoje.minusDays(1)),
                new ContratoAtivoView(vigenteId, tenantId, hoje.plusDays(1))));

        novoJob().executar();

        verify(contratoService).expirarSeAplicavel(eq(vencidoId), eq(hoje));
        verify(contratoService, never()).expirarSeAplicavel(eq(vigenteId), any());
    }

    @Test
    @DisplayName("resolve o fuso de cada tenant só uma vez por execução")
    void resolveFusoUmaVezPorTenant() {
        UUID tenantId = UUID.randomUUID();
        when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
        LocalDate ontem = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        when(queryRepository.buscarAtivosDeTodosOsTenants()).thenReturn(List.of(
                new ContratoAtivoView(UUID.randomUUID(), tenantId, ontem),
                new ContratoAtivoView(UUID.randomUUID(), tenantId, ontem)));

        novoJob().executar();

        verify(imobiliariaFacade, times(1)).fusoHorario(tenantId);
    }

    @Test
    @DisplayName("sem candidatos vencidos, não chama o serviço de expiração")
    void semCandidatosVencidosNaoChamaServico() {
        when(queryRepository.buscarAtivosDeTodosOsTenants()).thenReturn(List.of());

        novoJob().executar();

        verify(contratoService, never()).expirarSeAplicavel(any(), any());
    }
}
