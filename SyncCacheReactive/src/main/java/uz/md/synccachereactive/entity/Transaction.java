package uz.md.synccachereactive.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * We get from client services and save our repository as Transaction
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table
@Builder
@ToString
public class Transaction {

    @Id
    private Long id;

    private BigDecimal amount;

    private String fromCard; // transaction from

    private String toCard; // transaction to

    private TransactionStatus status; // transaction status

    private LocalDateTime addedDate = LocalDateTime.now(); // date

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction transaction = (Transaction) o;
        return Objects.equals(fromCard,transaction.fromCard)
                && Objects.equals(toCard, transaction.toCard)
                && Objects.equals(addedDate, transaction.addedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromCard, toCard, addedDate);
    }
}
