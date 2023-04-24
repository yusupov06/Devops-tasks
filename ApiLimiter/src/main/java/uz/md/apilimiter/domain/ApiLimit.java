package uz.md.apilimiter.domain;

import jakarta.persistence.*;
import lombok.*;
import uz.md.apilimiter.domain.templ.AbsLongEntity;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@Table(uniqueConstraints = {
//        @UniqueConstraint(columnNames = {"api_regex", "tariff_id"})})
public class ApiLimit extends AbsLongEntity {

    @Column(unique = true, nullable = false)
    private String apiRegex;

    private Long limitCount;

    private int priority;

    @ManyToOne
    private Tariff tariff;

}
