package uz.md.templatetopdf.service;

import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;

import java.util.List;

public interface PdfParserService {

    PdfFile getFromTemplateAndForm(String filePath, FormDTO form);

    List<PdfFile> getFromTemplateAndForms(String filePath, List<FormDTO> forms);

}
