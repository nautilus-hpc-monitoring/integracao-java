package school.sptech.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import school.sptech.dto.relatorio.EmpresaRelatorioDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

public class RelatorioPdfService {

    private final TemplateEngine engine;

    public RelatorioPdfService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    public byte[] gerarBytes(EmpresaRelatorioDto dados) {
        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        context.setVariable("empresa", dados.razaoSocial());
        context.setVariable("periodo", dados.periodo());
        context.setVariable("alertas", dados.alertas());
        context.setVariable("total", dados.total());
        context.setVariable("criticos", dados.criticos());
        context.setVariable("alertasNivel", dados.alertasNivel());
        context.setVariable("atencao", dados.atencao());
        context.setVariable("logo", logoEmBase64());

        String html = engine.process("relatorio-alertas", context);

        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, "");
            builder.useFont(() -> recurso("/fonts/Exo2-Regular.ttf"), "Exo 2", 400, FontStyle.NORMAL, true);
            builder.useFont(() -> recurso("/fonts/Exo2-Bold.ttf"), "Exo 2", 700, FontStyle.NORMAL, true);
            builder.useFont(() -> recurso("/fonts/DejaVuSans.ttf"), "DejaVu Sans", 400, FontStyle.NORMAL, true);
            builder.useFont(() -> recurso("/fonts/DejaVuSans-Bold.ttf"), "DejaVu Sans", 700, FontStyle.NORMAL, true);
            builder.toStream(saida);
            builder.run();
            return saida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erro gerando PDF do relatório", e);
        }
    }

    public void gerar(EmpresaRelatorioDto dados, Path destino) {
        try {
            Files.write(destino, gerarBytes(dados));
        } catch (IOException e) {
            throw new IllegalStateException("Erro gravando PDF do relatório em " + destino, e);
        }
    }

    private InputStream recurso(String caminho) {
        InputStream stream = RelatorioPdfService.class.getResourceAsStream(caminho);
        if (stream == null) {
            throw new IllegalStateException("Recurso não encontrado: " + caminho);
        }
        return stream;
    }

    private String logoEmBase64() {
        try (InputStream logo = recurso("/images/logo.png")) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(logo.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Erro lendo o logo do relatório", e);
        }
    }
}
