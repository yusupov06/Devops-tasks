package uz.md.templatetopdf.parser.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.util.ParserUtils;
import uz.md.templatetopdf.service.PdfFileService;
import uz.md.templatetopdf.util.ApplicationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

@Service(JsonParser.BEAN_NAME)
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("unchecked")
public class JsonParser implements Parser {


    public static final String BEAN_NAME = "JSON" + ApplicationProperties.PARSER_PREFIX;

    private final PdfFileService pdfFileService;

    private final ObjectMapper objectMapper;

    private final ParserUtils parserUtils;

    @Override
    public List<String> getFileExtensions() {
        return List.of("json");
    }

    @Override
    public PdfFile parse(File file, FormDTO form) throws Exception {

        Map<String, String> jsonMap = objectMapper.readValue(file, Map.class);

        // Convert the JSON tree to a string
        String jsonString = parserUtils.fillJsonTemplateWithForm(jsonMap, form);

        // Generate the PDF
        String fileName = parserUtils.getFileName(file);

        File outputPdf = pdfFileService.createFile(fileName);

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPdf));
        document.open();
        Paragraph paragraph = new Paragraph(jsonString);
        document.addAuthor("Muhammadqodir");
        document.add(paragraph);
        document.close();

        return pdfFileService.save(outputPdf, file.getAbsolutePath());
    }
}
