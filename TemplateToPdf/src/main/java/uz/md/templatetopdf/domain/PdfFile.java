package uz.md.templatetopdf.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PdfFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filename;
    private String path;
    private String templateRootPath;

    public PdfFile setId(Long id) {
        this.id = id;
        return this;
    }

    public PdfFile setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public PdfFile setPath(String path) {
        this.path = path;
        return this;
    }

    public PdfFile setTemplateRootPath(String templateRootPath) {
        this.templateRootPath = templateRootPath;
        return this;
    }
}
