package uz.md.templatetopdf.parser.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.TemplateExpression;
import uz.md.templatetopdf.service.TemplateExpressionService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class ExpressionsUtils {

    private final TemplateExpressionService templateExpressionService;

    public Optional<String> getValueForExpression(String expression) {
        TemplateExpression templateExpression = templateExpressionService.getByExpression(expression)
                .orElse(null);
        if (templateExpression == null) return Optional.empty();
        return Optional.ofNullable(templateExpression.getValue());
    }
}
