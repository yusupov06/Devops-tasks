package uz.md.templatetopdf.parser.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uz.md.templatetopdf.parser.Parser;
import uz.md.templatetopdf.parser.impl.CommonParser;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final ApplicationContext applicationContext;

    public Parser getParser(String fileExtension) {
        return applicationContext.getBeansOfType(Parser.class)
                .values()
                .stream()
                .filter(parser -> parser.getFileExtensions().contains(fileExtension))
                .findFirst()
                .orElse((Parser) applicationContext.getBean(CommonParser.BEAN_NAME));
    }
}
