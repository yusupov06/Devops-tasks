package uz.md.templatetopdf.parser.impl;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.util.ParserUtils;
import uz.md.templatetopdf.service.PdfFileService;
import uz.md.templatetopdf.util.ApplicationProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

@Service(WordParser.BEAN_NAME)
@RequiredArgsConstructor
@Transactional
public class WordParser implements Parser {

    public static final String BEAN_NAME = "WORD" + ApplicationProperties.PARSER_PREFIX;

    private final PdfFileService pdfFileService;
    private final ParserUtils parserUtils;

    @Override
    public List<String> getFileExtensions() {
        return List.of("doc", "docx");
    }

    @Override
    public PdfFile parse(File file, FormDTO form) throws Exception {

        // Load input Word document
        XWPFDocument document = new XWPFDocument(new FileInputStream(file));

        parserUtils.editWordDocumentWithForm(document, form);

        String fileName = parserUtils.getFileName(file);

        File outputPdf = pdfFileService.createFile(fileName);

        // Create output PDF file
        Document pdfDocument = new Document();
        PdfWriter.getInstance(pdfDocument, new FileOutputStream(outputPdf));
        pdfDocument.open();

        fillPdfDocument(pdfDocument, document.getParagraphs());

        // Close input Word document and output PDF file
        document.close();
        pdfDocument.close();

        return pdfFileService.save(outputPdf, file.getAbsolutePath());

    }

    private void fillPdfDocument(Document pdfDocument, List<XWPFParagraph> paragraphs) {

        // Loop through paragraphs in Word document and add to PDF document
        paragraphs.forEach(paragraph -> {
            Paragraph pdfParagraph = new Paragraph(paragraph.getText());
            pdfParagraph.setAlignment(paragraph.getAlignment().getValue());

//            // Iterate through the runs in the paragraph
//            for (XWPFRun run : paragraph.getRuns()) {
//                String text = run.getText(0);
//                Font font = new Font(Font.FontFamily.TIMES_ROMAN);
//                BaseColor color = BaseColor.BLACK;
//                if (run.getColor() != null) {
//                    int colorInt = Integer.parseInt(run.getColor(), 16);
//                    color = new BaseColor(colorInt);
//                }
//                font.setColor(color);
//                Chunk chunk = new Chunk(text, font);
//                pdfParagraph.add(chunk);
//            }

            pdfParagraph.setIndentationLeft(paragraph.getIndentationLeft());
            pdfParagraph.setIndentationRight(paragraph.getIndentationRight());
            pdfParagraph.setFirstLineIndent(paragraph.getFirstLineIndent());
            try {
                pdfDocument.add(pdfParagraph);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
