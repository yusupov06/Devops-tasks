package uz.md.templatetopdf.parser.impl;

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
import uz.md.templatetopdf.repository.PdfFileRepository;
import uz.md.templatetopdf.service.PdfFileService;
import uz.md.templatetopdf.util.ApplicationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Common parser for file extensions that are not supported by one of implementations
 */
@Service(CommonParser.BEAN_NAME)
@RequiredArgsConstructor
@Transactional
public class CommonParser implements Parser {

    // Bean name
    public static final String BEAN_NAME = "COMMON" + ApplicationProperties.PARSER_PREFIX;
    // beans
    private final ParserUtils parserUtils;
    private final PdfFileService pdfFileService;

    @Override
    public List<String> getFileExtensions() {
        return List.of();
    }

    /**
     * Main parser method
     * @param file template file
     * @param form form for filling template
     * @return {@link PdfFile}
     * @throws Exception error occurs
     */
    @Override
    public PdfFile parse(File file, FormDTO form) throws Exception {

        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));

        String modified = parserUtils.editWithForm(content, form);

        String fileName = parserUtils.getFileName(file);

        File outputPdf = pdfFileService.createFile(fileName);

        // Create output PDF file
        Document pdfDocument = new Document();
        PdfWriter.getInstance(pdfDocument, new FileOutputStream(outputPdf));
        pdfDocument.open();

        Scanner scanner = new Scanner(modified);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            pdfDocument.add(new Paragraph(line));
        }

        scanner.close();
        pdfDocument.close();

        return pdfFileService.save(outputPdf, file.getAbsolutePath());
    }
}
