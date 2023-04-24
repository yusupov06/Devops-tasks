package uz.md.synccachereactive.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class GetByDatesRequest {

    @NotNull(message = "card numbers is required")
    private List<String> cardNumbers;
    @NotNull(message = " FromDate can not be null")
    private LocalDateTime dateFrom;
    @NotNull(message = " ToDate can not be null")
    private LocalDateTime dateTo;

}
