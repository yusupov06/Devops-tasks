package uz.md.synccachereactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transaction, Long> {

    @Query("select * from transaction where (from_card = :card or to_card = :card) " +
            "and transaction.added_date >= :fromDate and transaction.added_date <= :toDate order by added_date")
    Flux<Transaction> findAllByCardAndAddedDateBetween(String card,
                                                       LocalDateTime fromDate,
                                                       LocalDateTime toDate);

    Mono<Transaction> findByAddedDate(LocalDateTime addedDate);

    @Query("SELECT EXISTS(SELECT 1 FROM public.transaction WHERE from_card = :fromCard and to_card = :toCard and added_date = :addedDate);")
    Mono<Boolean> existsByFromCardAndToCardAndAddedDate(String fromCard, String toCard, LocalDateTime addedDate);

    Mono<Transaction> findByFromCardAndToCardAndAddedDate(String fromCard, String toCard, LocalDateTime addedDate);

    @Query("select * from public.transaction order by added_date")
    Flux<Transaction> findAllSorted();
}
