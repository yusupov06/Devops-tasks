package uz.md.templatetopdf.util;

import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {

    public String getFileExtension(String templatePath) {
        return templatePath.substring(templatePath.lastIndexOf("."));
    }
}
