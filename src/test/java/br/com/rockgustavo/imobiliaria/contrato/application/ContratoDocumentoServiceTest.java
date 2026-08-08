package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.StatusContrato;
import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade.Identificacao;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade.Qualificacao;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade.QualificacaoImovel;
import br.com.rockgustavo.imobiliaria.shared.documento.HtmlParaPdf;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoDocumentoServiceTest {

    private static final TemplateEngine TEMPLATE_ENGINE = criarTemplateEngine();
    private static final HtmlParaPdf HTML_PARA_PDF = new HtmlParaPdf();

    @Mock
    ContratoService contratoService;

    @Mock
    ImobiliariaFacade imobiliariaFacade;

    @Mock
    PessoaFacade pessoaFacade;

    @Mock
    PropriedadeFacade propriedadeFacade;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID contratoId = UUID.randomUUID();
    private final UUID pessoaId = UUID.randomUUID();
    private final UUID propriedadeId = UUID.randomUUID();

    private ContratoDocumentoService novoServico() {
        return new ContratoDocumentoService(contratoService, imobiliariaFacade, pessoaFacade, propriedadeFacade,
                TEMPLATE_ENGINE, HTML_PARA_PDF);
    }

    @BeforeEach
    void definirTenant() {
        TenantContext.definir(tenantId);
        lenient().when(imobiliariaFacade.identificacao(tenantId))
                .thenReturn(new Identificacao("Imobiliária Demo Ltda", "12345678000190"));
        lenient().when(imobiliariaFacade.fusoHorario(tenantId)).thenReturn("America/Sao_Paulo");
        lenient().when(pessoaFacade.qualificacao(pessoaId))
                .thenReturn(Optional.of(new Qualificacao("Maria Silva", "CPF", "52998224725")));
        lenient().when(propriedadeFacade.qualificacoes(tenantId, List.of(propriedadeId)))
                .thenReturn(Map.of(propriedadeId, new QualificacaoImovel(
                        "APARTAMENTO", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP")));
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Test
    @DisplayName("gera PDF do estado atual com timbre, partes, objeto e vigência formatados")
    void geraPdfComEstadoAtualFormatado() throws IOException {
        ContratoDetalhe detalhe = umContratoDetalhe(new BigDecimal("5.00"), List.of());
        when(contratoService.buscarPorId(contratoId)).thenReturn(detalhe);

        byte[] pdf = novoServico().gerar(contratoId, null);
        String texto = extrairTexto(pdf);

        assertThat(pdf).startsWith("%PDF".getBytes());
        assertThat(texto).contains("Imobiliária Demo Ltda", "12.345.678/0001-90");
        assertThat(texto).contains("Maria Silva", "529.982.247-25");
        assertThat(texto).contains("Av. Paulista, 1000", "Bela Vista, São Paulo/SP");
        assertThat(texto).contains("5,00%", "R$ 450.000,00");
        assertThat(texto).contains("20/08/2026", "20/08/2027");
        assertThat(texto).contains("ATIVO");
        assertThat(texto).contains("Documento emitido em");
        assertThat(texto).doesNotContain("registro histórico");
        verify(contratoService, never()).buscarHistoricoEm(any(), any());
    }

    @Test
    @DisplayName("RN-09-03: com data, usa o snapshot histórico e adiciona a nota de ressalva")
    void geraPdfComDataUsaSnapshotHistoricoENotaDeRessalva() throws IOException {
        LocalDate data = LocalDate.of(2026, 3, 15);
        ContratoDetalhe detalheHistorico = umContratoDetalhe(new BigDecimal("4.00"), List.of());
        when(contratoService.buscarHistoricoEm(contratoId, data))
                .thenReturn(new ContratoHistoricoDetalhe(1, Instant.now(), detalheHistorico));

        byte[] pdf = novoServico().gerar(contratoId, data);
        String texto = extrairTexto(pdf);

        assertThat(texto).contains("4,00%");
        assertThat(texto).contains("registro histórico de 15/03/2026");
        assertThat(texto).contains("cadastro atual");
        verify(contratoService, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("RN-06-08: lista aditivos com o rótulo traduzido e a justificativa")
    void geraPdfComAditivosListaOsAditivos() throws IOException {
        AditivoDetalhe aditivo = new AditivoDetalhe(propriedadeId, TipoAditivo.INCLUSAO,
                "renegociação de comissão", LocalDate.of(2026, 9, 2));
        ContratoDetalhe detalhe = umContratoDetalhe(new BigDecimal("5.00"), List.of(aditivo));
        when(contratoService.buscarPorId(contratoId)).thenReturn(detalhe);

        String texto = extrairTexto(novoServico().gerar(contratoId, null));

        assertThat(texto).contains("Inclusão", "renegociação de comissão", "02/09/2026");
    }

    @Test
    @DisplayName("propriedade sem qualificação resolvida usa texto de reserva em vez de quebrar a geração")
    void propriedadeSemQualificacaoUsaFallback() throws IOException {
        ContratoDetalhe detalhe = umContratoDetalhe(new BigDecimal("5.00"), List.of());
        when(contratoService.buscarPorId(contratoId)).thenReturn(detalhe);
        when(propriedadeFacade.qualificacoes(tenantId, List.of(propriedadeId))).thenReturn(Map.of());

        String texto = extrairTexto(novoServico().gerar(contratoId, null));

        assertThat(texto).contains("Propriedade " + propriedadeId);
    }

    @Test
    @DisplayName("pessoa referenciada pelo contrato não encontrada é uma falha de invariante, não de negócio")
    void pessoaNaoEncontradaLancaIllegalStateException() {
        ContratoDetalhe detalhe = umContratoDetalhe(new BigDecimal("5.00"), List.of());
        when(contratoService.buscarPorId(contratoId)).thenReturn(detalhe);
        when(pessoaFacade.qualificacao(pessoaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> novoServico().gerar(contratoId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(pessoaId.toString());
    }

    private ContratoDetalhe umContratoDetalhe(BigDecimal comissaoPercentual, List<AditivoDetalhe> aditivos) {
        AgenciamentoDetalhe agenciamento = new AgenciamentoDetalhe(
                UUID.randomUUID(), propriedadeId, comissaoPercentual, new BigDecimal("450000.00"), true);
        return new ContratoDetalhe(contratoId, pessoaId, UUID.randomUUID(), StatusContrato.ATIVO,
                LocalDate.of(2026, 8, 20), LocalDate.of(2027, 8, 20), "Regras contratuais de teste.", null,
                List.of(agenciamento), aditivos, Instant.now(), Instant.now());
    }

    private static String extrairTexto(byte[] pdf) throws IOException {
        try (PDDocument documento = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(documento).replace(' ', ' ');
        }
    }

    private static TemplateEngine criarTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
