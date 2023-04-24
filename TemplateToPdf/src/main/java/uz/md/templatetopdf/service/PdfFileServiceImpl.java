package uz.md.templatetopdf.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.repository.PdfFileRepository;

import java.io.File;

@Service
public class PdfFileServiceImpl implements PdfFileService {

    @Value("${app.pdf-file.root}")
    public String pdfFileRootPath;

    private final PdfFileRepository pdfFileRepository;

    public PdfFileServiceImpl(PdfFileRepository pdfFileRepository) {
        this.pdfFileRepository = pdfFileRepository;
    }

    @Override
    public String getFileRootPath() {
        return pdfFileRootPath;
    }

    @Override
    public PdfFile save(File outputPdf, String templateRootPath) {

        PdfFile pdfFile = new PdfFile()
                .setPath(pdfFileRootPath + outputPdf.getName())
                .setFilename(outputPdf.getName())
                .setTemplateRootPath(templateRootPath);

        return pdfFileRepository.save(pdfFile);
    }

    @Override
    public File createFile(String fileName) {
        return new File(pdfFileRootPath + fileName + ".pdf");
    }
}
