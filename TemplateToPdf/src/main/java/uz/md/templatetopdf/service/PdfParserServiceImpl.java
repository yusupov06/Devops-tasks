package uz.md.templatetopdf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.factory.ParserFactory;
import uz.md.templatetopdf.util.ServiceUtil;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PdfParserServiceImpl implements PdfParserService {

    private final ParserFactory parserFactory;
    private final ServiceUtil serviceUtil;

    @Override
    public PdfFile getFromTemplateAndForm(String templatePath, FormDTO form) {
        File template = new File(templatePath);
        String fileExtension = serviceUtil.getFileExtension(templatePath);
        Parser parser = parserFactory.getParser(fileExtension);
        try {
            return parser.parse(template, form);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PdfFile> getFromTemplateAndForms(String templatePath, List<FormDTO> forms) {

        File template = new File(templatePath);
        String fileExtension = serviceUtil.getFileExtension(templatePath);
        Parser parser = parserFactory.getParser(fileExtension);

        return forms.stream()
                .map(formDTO -> {
                    try {
                        return parser.parse(template, formDTO);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
