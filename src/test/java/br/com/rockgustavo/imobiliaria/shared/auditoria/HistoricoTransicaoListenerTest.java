package br.com.rockgustavo.imobiliaria.shared.auditoria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RN-09-01: ordem da trilha de auditoria")
class HistoricoTransicaoListenerTest {

    @Mock
    HistoricoTransicaoRepository repository;

    @Test
    @DisplayName("grava o instante em que a transição ocorreu, não o do processamento assíncrono")
    void gravaOInstanteDaTransicaoNaoODoProcessamento() {
        Instant quandoATransicaoOcorreu = Instant.now().minus(2, ChronoUnit.MINUTES);
        TransicaoStatusOcorrida evento = new TransicaoStatusOcorrida(UUID.randomUUID(), EntidadeAuditavel.CONTRATO,
                UUID.randomUUID(), "RASCUNHO", "ATIVO", UUID.randomUUID(), quandoATransicaoOcorreu);

        new HistoricoTransicaoListener(repository).aoTransicionar(evento);

        ArgumentCaptor<HistoricoTransicao> gravado = ArgumentCaptor.forClass(HistoricoTransicao.class);
        verify(repository).save(gravado.capture());
        assertThat(gravado.getValue().getOcorridoEm()).isEqualTo(quandoATransicaoOcorreu);
    }
}
