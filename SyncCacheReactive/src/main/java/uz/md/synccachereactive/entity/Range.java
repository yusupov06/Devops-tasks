package uz.md.synccachereactive.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Range for cache we get cache range
 * When we call card is 8600 and dates [1-3]
 * We save cache range as this card is cached with fromDate 1 and toDate 3
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table
@Getter
@Setter
@ToString
public class Range {

    @Id
    private Long id;

    private String cardNumber;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    public Range(LocalDateTime fromDate, LocalDateTime toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

}
