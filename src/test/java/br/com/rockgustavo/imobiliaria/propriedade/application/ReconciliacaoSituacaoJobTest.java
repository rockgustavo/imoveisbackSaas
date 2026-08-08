package br.com.rockgustavo.imobiliaria.propriedade.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeAgenciadaView;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeQueryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RN-03-09: reconciliação entre situação da propriedade e agenciamento vigente")
class ReconciliacaoSituacaoJobTest {

    @Mock
    PropriedadeQueryRepository queryRepository;

    private ListAppender<ILoggingEvent> divergenciasReportadas;
    private Logger logger;

    @BeforeEach
    void capturarLog() {
        divergenciasReportadas = new ListAppender<>();
        divergenciasReportadas.start();
        logger = (Logger) LoggerFactory.getLogger(ReconciliacaoSituacaoJob.class);
        logger.addAppender(divergenciasReportadas);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(divergenciasReportadas);
    }

    @Nested
    @DisplayName("critérios de aceite do backlog")
    class CriteriosDeAceite {

        @Test
        @DisplayName("propriedade AGENCIADA com agenciamento vigente não reporta divergência")
        void agenciadaComAgenciamentoVigenteNaoReportaDivergencia() {
            UUID tenantId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(queryRepository.propriedadesAgenciadasDeTodosOsTenants())
                    .thenReturn(List.of(new PropriedadeAgenciadaView(propriedadeId, tenantId)));
            when(queryRepository.propriedadesComAgenciamentoVigenteDeTodosOsTenants())
                    .thenReturn(Set.of(propriedadeId));

            new ReconciliacaoSituacaoJob(queryRepository).executar();

            assertThat(divergencias()).isEmpty();
        }

        @Test
        @DisplayName("propriedade AGENCIADA sem agenciamento vigente é reportada para investigação manual")
        void agenciadaSemAgenciamentoVigenteEhReportada() {
            UUID tenantId = UUID.randomUUID();
            UUID propriedadeId = UUID.randomUUID();
            when(queryRepository.propriedadesAgenciadasDeTodosOsTenants())
                    .thenReturn(List.of(new PropriedadeAgenciadaView(propriedadeId, tenantId)));
            when(queryRepository.propriedadesComAgenciamentoVigenteDeTodosOsTenants()).thenReturn(Set.of());

            new ReconciliacaoSituacaoJob(queryRepository).executar();

            assertThat(divergencias()).singleElement().asString().contains(propriedadeId.toString());
        }
    }

    @Test
    @DisplayName("agenciamento vigente sobre propriedade que não está AGENCIADA também é reportado")
    void agenciamentoVigenteSemSituacaoAgenciadaEhReportado() {
        UUID propriedadeId = UUID.randomUUID();
        when(queryRepository.propriedadesAgenciadasDeTodosOsTenants()).thenReturn(List.of());
        when(queryRepository.propriedadesComAgenciamentoVigenteDeTodosOsTenants()).thenReturn(Set.of(propriedadeId));

        new ReconciliacaoSituacaoJob(queryRepository).executar();

        assertThat(divergencias()).singleElement().asString().contains(propriedadeId.toString());
    }

    @Test
    @DisplayName("não reporta divergência quando não há propriedade agenciada nem agenciamento vigente")
    void semDadoNaoReportaDivergencia() {
        when(queryRepository.propriedadesAgenciadasDeTodosOsTenants()).thenReturn(List.of());
        when(queryRepository.propriedadesComAgenciamentoVigenteDeTodosOsTenants()).thenReturn(Set.of());

        new ReconciliacaoSituacaoJob(queryRepository).executar();

        assertThat(divergencias()).isEmpty();
    }

    private List<String> divergencias() {
        return divergenciasReportadas.list.stream()
                .filter(evento -> evento.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
