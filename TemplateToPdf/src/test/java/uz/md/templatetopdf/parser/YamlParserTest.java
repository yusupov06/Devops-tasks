package uz.md.templatetopdf.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.repository.PdfFileRepository;
import uz.md.templatetopdf.service.PdfFileService;

import java.io.File;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("testdev")
@Transactional
public class YamlParserTest {

    @Autowired
    @Qualifier("YAML_PARSER")
    private Parser parser;

    @Autowired
    private PdfFileService pdfFileService;

    @Autowired
    private PdfFileRepository pdfFileRepository;


    @Test
    void shouldWork() throws Exception {
        File file = new File("src/test/resources/sample/sample.yml");

        Map<String, String> map = Map.of(
                "name", "Muhammadqodir",
                "surname", "Yusupov",
                "age", "30"
        );

        FormDTO formDTO = new FormDTO();
        formDTO.setFormBody(map);

        PdfFile parsed = parser.parse(file, formDTO);

        List<PdfFile> all = pdfFileRepository.findAll();
        Assertions.assertNotNull(all);
        Assertions.assertEquals(all.size(), 1);
        PdfFile pdfFile = all.get(0);
        Assertions.assertNotNull(pdfFile);
        Assertions.assertNotNull(parsed);
        Assertions.assertEquals(parsed.getPath(), pdfFileService.getFileRootPath() + "sample.pdf");

    }



}
