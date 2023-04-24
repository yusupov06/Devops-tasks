package uz.md.templatetopdf.service;

import uz.md.templatetopdf.domain.PdfFile;

import java.io.File;

public interface PdfFileService {

    String getFileRootPath();

    PdfFile save(File outputPdf, String templateRootPath);

    File createFile(String fileName);

}
