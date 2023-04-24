package uz.md.synccache.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class GetByDateRequest {
    @NotNull(message = " FromDate can not be null")
    LocalDate dateFrom;
    @NotNull(message = " ToDate can not be null")
    LocalDate dateTo;

}
