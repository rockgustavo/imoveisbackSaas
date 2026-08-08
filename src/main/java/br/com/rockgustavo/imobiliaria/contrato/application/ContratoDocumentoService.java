package br.com.rockgustavo.imobiliaria.contrato.application;

import br.com.rockgustavo.imobiliaria.contrato.domain.TipoAditivo;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade.Identificacao;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade;
import br.com.rockgustavo.imobiliaria.pessoa.PessoaFacade.Qualificacao;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade;
import br.com.rockgustavo.imobiliaria.propriedade.PropriedadeFacade.QualificacaoImovel;
import br.com.rockgustavo.imobiliaria.shared.documento.HtmlParaPdf;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ContratoDocumentoService {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Map<TipoAditivo, String> ROTULO_ADITIVO = Map.of(
            TipoAditivo.INCLUSAO, "Inclusão",
            TipoAditivo.EXCLUSAO, "Exclusão");
    private static final String TEMPLATE_INSTRUMENTO = "contrato/instrumento";

    private final ContratoService contratoService;
    private final ImobiliariaFacade imobiliariaFacade;
    private final PessoaFacade pessoaFacade;
    private final PropriedadeFacade propriedadeFacade;
    private final TemplateEngine templateEngine;
    private final HtmlParaPdf htmlParaPdf;

    public ContratoDocumentoService(ContratoService contratoService, ImobiliariaFacade imobiliariaFacade,
                                     PessoaFacade pessoaFacade, PropriedadeFacade propriedadeFacade,
                                     TemplateEngine templateEngine, HtmlParaPdf htmlParaPdf) {
        this.contratoService = contratoService;
        this.imobiliariaFacade = imobiliariaFacade;
        this.pessoaFacade = pessoaFacade;
        this.propriedadeFacade = propriedadeFacade;
        this.templateEngine = templateEngine;
        this.htmlParaPdf = htmlParaPdf;
    }

    @PreAuthorize("hasAnyRole('USUARIO', 'ADMINISTRADOR')")
    public byte[] gerar(UUID id, LocalDate data) {
        ContratoDetalhe detalhe = data == null
                ? contratoService.buscarPorId(id)
                : contratoService.buscarHistoricoEm(id, data).contrato();
        ContratoDocumento documento = montarDocumento(detalhe, data);
        String html = templateEngine.process(TEMPLATE_INSTRUMENTO, new Context(LOCALE_PT_BR, Map.of("doc", documento)));
        return htmlParaPdf.renderizar(html);
    }

    private ContratoDocumento montarDocumento(ContratoDetalhe detalhe, LocalDate data) {
        UUID tenantId = TenantContext.obter();
        Identificacao identificacao = imobiliariaFacade.identificacao(tenantId);
        Qualificacao contratante = pessoaFacade.qualificacao(detalhe.pessoaId())
                .orElseThrow(() -> new IllegalStateException(
                        "Pessoa %s referenciada pelo contrato %s não foi encontrada".formatted(detalhe.pessoaId(), detalhe.id())));

        List<UUID> propriedadeIds = detalhe.agenciamentos().stream().map(AgenciamentoDetalhe::propriedadeId).toList();
        Map<UUID, QualificacaoImovel> imoveis = propriedadeFacade.qualificacoes(tenantId, propriedadeIds);

        List<ContratoDocumento.ItemObjeto> itens = detalhe.agenciamentos().stream()
                .map(agenciamento -> paraItemObjeto(agenciamento, imoveis))
                .toList();
        List<ContratoDocumento.ItemAditivo> aditivos = detalhe.aditivos().stream()
                .map(ContratoDocumentoService::paraItemAditivo)
                .toList();

        return new ContratoDocumento(
                numeroDoContrato(detalhe.id()),
                detalhe.status().name(),
                identificacao.razaoSocial(),
                formatarCnpj(identificacao.cnpj()),
                contratante.nome(),
                formatarDocumento(contratante.tipoDocumento(), contratante.documento()),
                itens,
                formatarData(detalhe.vigenciaInicio()),
                formatarData(detalhe.vigenciaFim()),
                detalhe.regrasContratuais(),
                aditivos,
                emitidoEmAgora(tenantId),
                notaHistorica(data));
    }

    private ContratoDocumento.ItemObjeto paraItemObjeto(AgenciamentoDetalhe agenciamento, Map<UUID, QualificacaoImovel> imoveis) {
        QualificacaoImovel imovel = imoveis.get(agenciamento.propriedadeId());
        String tipo = imovel == null ? "—" : capitalizar(imovel.tipo());
        String endereco = imovel == null ? "Propriedade " + agenciamento.propriedadeId() : formatarEndereco(imovel);
        return new ContratoDocumento.ItemObjeto(tipo, endereco,
                formatarPercentual(agenciamento.comissaoPercentual()), formatarMoeda(agenciamento.valorPedido()));
    }

    private static ContratoDocumento.ItemAditivo paraItemAditivo(AditivoDetalhe aditivo) {
        return new ContratoDocumento.ItemAditivo(
                formatarData(aditivo.data()), ROTULO_ADITIVO.get(aditivo.tipo()), aditivo.justificativa());
    }

    private String emitidoEmAgora(UUID tenantId) {
        ZoneId fuso = ZoneId.of(imobiliariaFacade.fusoHorario(tenantId));
        return FORMATO_DATA_HORA.format(Instant.now().atZone(fuso));
    }

    private static String notaHistorica(LocalDate data) {
        if (data == null) {
            return null;
        }
        String modelo = "Documento gerado a partir do registro histórico de %s. Qualificação das partes e "
                + "endereços dos imóveis refletem o cadastro atual, não o da data pedida.";
        return modelo.formatted(formatarData(data));
    }

    private static String numeroDoContrato(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String formatarEndereco(QualificacaoImovel imovel) {
        StringBuilder endereco = new StringBuilder(imovel.logradouro()).append(", ").append(imovel.numero());
        if (imovel.complemento() != null && !imovel.complemento().isBlank()) {
            endereco.append(" - ").append(imovel.complemento());
        }
        endereco.append(" — ").append(imovel.bairro()).append(", ").append(imovel.localidade())
                .append("/").append(imovel.uf());
        return endereco.toString();
    }

    private static String formatarDocumento(String tipoDocumento, String documento) {
        return "CNPJ".equals(tipoDocumento) ? formatarCnpj(documento) : formatarCpf(documento);
    }

    private static String formatarCpf(String cpf) {
        return "%s.%s.%s-%s".formatted(cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9), cpf.substring(9, 11));
    }

    private static String formatarCnpj(String cnpj) {
        return "%s.%s.%s/%s-%s".formatted(cnpj.substring(0, 2), cnpj.substring(2, 5), cnpj.substring(5, 8),
                cnpj.substring(8, 12), cnpj.substring(12, 14));
    }

    private static String formatarMoeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_PT_BR).format(valor);
    }

    private static String formatarPercentual(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(LOCALE_PT_BR);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor) + "%";
    }

    private static String formatarData(LocalDate data) {
        return data.format(FORMATO_DATA);
    }

    private static String capitalizar(String valor) {
        return valor.charAt(0) + valor.substring(1).toLowerCase(LOCALE_PT_BR);
    }
}
