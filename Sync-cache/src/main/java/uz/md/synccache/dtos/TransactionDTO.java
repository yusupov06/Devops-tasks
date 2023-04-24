package uz.md.synccache.dtos;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TransactionDTO {
    private Long id;
    private BigDecimal amount;
    private String fromCard; // transaction from
    private String toCard; // transaction to
    private LocalDateTime addedDate;
    private String status; // transaction status

}
