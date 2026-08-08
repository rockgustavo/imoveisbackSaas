package br.com.rockgustavo.imobiliaria.shared.documento;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Component
public class HtmlParaPdf {

    private static final String FONT_FAMILY = "Noto Sans";
    private static final String FONTE_REGULAR = "fonts/NotoSans-Regular.ttf";
    private static final String FONTE_NEGRITO = "fonts/NotoSans-Bold.ttf";

    public byte[] renderizar(String html) {
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.useFont(() -> abrirFonte(FONTE_REGULAR), FONT_FAMILY, 400, FontStyle.NORMAL, true);
            builder.useFont(() -> abrirFonte(FONTE_NEGRITO), FONT_FAMILY, 700, FontStyle.NORMAL, true);
            builder.toStream(saida);
            builder.run();
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar PDF", e);
        }
    }

    private static InputStream abrirFonte(String caminhoNoClasspath) {
        try {
            return new ClassPathResource(caminhoNoClasspath).getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException("Fonte não encontrada no classpath: " + caminhoNoClasspath, e);
        }
    }
}
