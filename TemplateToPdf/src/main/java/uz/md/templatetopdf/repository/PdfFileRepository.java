package uz.md.templatetopdf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.md.templatetopdf.domain.PdfFile;

@Repository
public interface PdfFileRepository extends JpaRepository<PdfFile, Long> {
}