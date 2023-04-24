package uz.md.templatetopdf.parser.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.util.ParserUtils;
import uz.md.templatetopdf.service.PdfFileService;
import uz.md.templatetopdf.util.ApplicationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * HTML files parser.
 */
@Service(value = HtmlParser.BEAN_NAME)
@Transactional
@RequiredArgsConstructor
public class HtmlParser implements Parser {

    public static final String BEAN_NAME = "HTML" + ApplicationProperties.PARSER_PREFIX;
    private final PdfFileService pdfFileService;
    private final ParserUtils parserUtils;

    @Override
    public List<String> getFileExtensions() {
        return List.of("html");
    }

    /**
     * Main method for parsing
     * 1. It reads HTML and modify with form
     * 2. write modified html to pdf
     * @param file template
     * @param form key-value paired data
     * @return {@link PdfFile}
     * @throws IOException error occurred
     */
    @Override
    public PdfFile parse(File file, FormDTO form) throws IOException {

        Document document = Jsoup.parse(file, "UTF-8");
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml);

        String fileName = parserUtils.getFileName(file);

        String modifiedHtml = parserUtils.editHtmlBodyAndReturnEdited(document.html(), form);

        File outputPdf = pdfFileService.createFile(fileName);

        try (OutputStream outputStream = new FileOutputStream(outputPdf)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext sharedContext = renderer.getSharedContext();
            sharedContext.setPrint(true);
            sharedContext.setInteractive(false);
            renderer.setDocumentFromString(modifiedHtml);
            renderer.layout();
            renderer.createPDF(outputStream);
        }

        return pdfFileService.save(outputPdf, file.getAbsolutePath());

    }
}
