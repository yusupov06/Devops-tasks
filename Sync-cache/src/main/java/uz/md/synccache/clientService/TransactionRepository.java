package uz.md.synccache.clientService;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.md.synccache.entity.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByIdIsIn(Collection<Long> id);

    @Query("from Transaction where addedDate >= :fromDate and addedDate <= :toDate order by addedDate")
    List<Transaction> findAllByAddedDateBetween(LocalDate fromDate, LocalDate toDate);

}
