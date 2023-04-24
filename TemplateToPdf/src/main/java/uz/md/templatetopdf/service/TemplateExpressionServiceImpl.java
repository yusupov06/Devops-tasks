package uz.md.templatetopdf.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.templatetopdf.domain.TemplateExpression;
import uz.md.templatetopdf.error.AlreadyExistedException;
import uz.md.templatetopdf.error.NotFoundException;
import uz.md.templatetopdf.repository.TemplateExpressionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TemplateExpressionServiceImpl implements TemplateExpressionService {

    private final TemplateExpressionRepository templateExpressionRepository;

    public TemplateExpressionServiceImpl(TemplateExpressionRepository templateExpressionRepository) {
        this.templateExpressionRepository = templateExpressionRepository;
    }


    @Override
    public boolean add(String expression, String value) {

        if (templateExpressionRepository.existsByExpression(expression))
            throw new AlreadyExistedException("Template expression already exists");

        TemplateExpression expression1 = new TemplateExpression(expression, value);
        templateExpressionRepository.save(expression1);
        return true;
    }

    @Override
    public boolean edit(Long id, String expression, String value) {

        TemplateExpression templateExpression = templateExpressionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template Expression not found with id " + id));

        if (!expression.equals(templateExpression.getExpression()) &&
                templateExpressionRepository.existsByExpression(expression))
            throw new AlreadyExistedException("Template expression already exists");

        templateExpression.setExpression(expression);
        templateExpression.setValue(value);
        templateExpressionRepository.save(templateExpression);
        return true;
    }

    @Override
    public Map<String, String> getAll() {
        return templateExpressionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TemplateExpression::getExpression, TemplateExpression::getValue));
    }

    @Override
    public boolean delete(Long id) {
        templateExpressionRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean delete(String expression) {
        return templateExpressionRepository
                .deleteByExpression(expression);
    }

    @Override
    public Optional<TemplateExpression> getByExpression(String expression) {
        return templateExpressionRepository.findByExpression(expression);
    }
}
