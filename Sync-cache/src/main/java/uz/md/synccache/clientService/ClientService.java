package uz.md.synccache.clientService;


import uz.md.synccache.entity.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClientService {

    List<Transaction> getAllByDateBetween(LocalDate dateFrom, LocalDate dateTo);

    List<Transaction> getByDate(LocalDate dateTo);
}
