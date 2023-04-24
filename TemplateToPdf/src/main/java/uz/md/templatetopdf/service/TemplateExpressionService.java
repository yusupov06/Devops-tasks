package uz.md.templatetopdf.service;

import uz.md.templatetopdf.domain.TemplateExpression;

import java.util.Map;
import java.util.Optional;

public interface TemplateExpressionService {

    boolean add(String expression, String value);

    boolean edit(Long id, String expression, String value);

    Map<String, String> getAll();

    boolean delete(Long id);

    boolean delete(String expression);

    Optional<TemplateExpression> getByExpression(String expression);
}
