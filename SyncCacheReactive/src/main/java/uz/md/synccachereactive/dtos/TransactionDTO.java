package uz.md.synccachereactive.dtos;

import lombok.*;
import uz.md.synccachereactive.entity.TransactionStatus;

import java.math.BigDecimal;
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
    private TransactionStatus status; // transaction status

}
