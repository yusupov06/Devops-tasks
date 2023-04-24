package uz.md.templatetopdf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import uz.md.templatetopdf.domain.PdfFile;
import uz.md.templatetopdf.dto.FormDTO;
import uz.md.templatetopdf.parser.Parser;

import java.io.File;
import java.util.Map;

@SpringBootTest
class TemplateToPdfApplicationTests {

    @Autowired
    @Qualifier(value = "COMMON_PARSER")
    private Parser parser;

    @Test
    void contextLoads() {
    }

    @Test
    void test() throws Exception {

        File file = new File("src/test/resources/sample/sample.txt");

        Map<String, String> map = Map.of(
                "admin", "Yusupov",
                "name", "Muhammadqodir",
                "orderDate", "2022.03.03",
                "productName", "Apple",
                "productQuantity", "3",
                "productPrice", "21000"
        );

        FormDTO formDTO = new FormDTO();
        formDTO.setFormBody(map);

        PdfFile parse = parser.parse(file, formDTO);

    }

}
