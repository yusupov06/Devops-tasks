package uz.md.apilimiter.domain;


import jakarta.persistence.*;
import lombok.*;
import uz.md.apilimiter.domain.enums.TariffType;
import uz.md.apilimiter.domain.templ.AbsLongEntity;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tariff extends AbsLongEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TariffType type;

    private BigDecimal price;

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.PERSIST)
    private List<ApiLimit> limits;

    public Tariff(String name, TariffType type, BigDecimal price) {
        this.name = name;
        this.type = type;
        this.price = price;
    }
}
