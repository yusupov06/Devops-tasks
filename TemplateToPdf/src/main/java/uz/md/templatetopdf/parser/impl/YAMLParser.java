package uz.md.templatetopdf.parser.impl;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.util.ParserUtils;
import uz.md.templatetopdf.service.PdfFileService;
import uz.md.templatetopdf.util.ApplicationProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service(YAMLParser.BEAN_NAME)
@RequiredArgsConstructor
@Transactional
public class YAMLParser implements Parser {

    public static final String BEAN_NAME = "YAML" + ApplicationProperties.PARSER_PREFIX;

    private final ParserUtils parserUtils;
    private final PdfFileService pdfFileService;


    @Override
    public List<String> getFileExtensions() {
        return List.of("yaml", "yml");
    }

    @Override
    public PdfFile parse(File file, FormDTO form) throws Exception {

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(file);
        Map<String, Object> ymlMap = yaml.load(inputStream);
        parserUtils.fillYmlMapAndReturnModifiedMap(ymlMap, form);

        String fileName = parserUtils.getFileName(file);

        File outputPdf = pdfFileService.createFile(fileName);

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPdf));
        document.open();
        addToDocumentAsParagraph(document, ymlMap, 0);
        document.close();

        return pdfFileService.save(outputPdf, file.getAbsolutePath());

    }

    private void addToDocumentAsParagraph(Document document, Map<String, Object> map, int spaceCount) throws DocumentException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                document.add(new Paragraph(" ".repeat(spaceCount) + key + ":"));
                addToDocumentAsParagraph(document, (Map<String, Object>) value, spaceCount + key.length());
            } else {
                document.add(new Paragraph(" ".repeat(spaceCount) + key + ": " + value));
            }
        }
    }
}
