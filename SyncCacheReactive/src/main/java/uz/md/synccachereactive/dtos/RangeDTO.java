package uz.md.synccachereactive.dtos;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RangeDTO {
    private String cardNumber;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
