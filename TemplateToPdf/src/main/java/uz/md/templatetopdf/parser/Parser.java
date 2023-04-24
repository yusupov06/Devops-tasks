package uz.md.templatetopdf.parser;

import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;

import java.io.File;
import java.util.List;

/**
 * Parser for Parsing desired file to PDF
 */
public interface Parser {

    List<String> getFileExtensions();

    PdfFile parse(File file, FormDTO form) throws Exception;
}
