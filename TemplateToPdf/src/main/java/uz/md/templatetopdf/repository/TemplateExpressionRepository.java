package uz.md.templatetopdf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.md.templatetopdf.domain.TemplateExpression;

import java.util.Optional;

@Repository
public interface TemplateExpressionRepository extends JpaRepository<TemplateExpression, Long> {
    boolean existsByExpression(String expression);

    boolean deleteByExpression(String expression);

    Optional<TemplateExpression> findByExpression(String expression);

}