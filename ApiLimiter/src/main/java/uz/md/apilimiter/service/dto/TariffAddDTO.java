package uz.md.apilimiter.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.md.apilimiter.domain.enums.TariffType;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TariffAddDTO {
    private String name;
    private TariffType type;
    private BigDecimal price;

}
