package uz.md.synccache.clientService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> getAllByDateBetween(LocalDate dateFrom, LocalDate dateTo) {

        if (dateFrom == null || dateTo == null)
            throw new BadRequestException("Dates cannot be null");

        log.info(" finding all by date between " + dateFrom + " and " + dateTo);
        return transactionRepository
                .findAllByAddedDateBetween(dateFrom, dateTo);
    }


    @Override
    public List<Transaction> getByDate(LocalDate addedDate) {
        return transactionRepository.findAllByAddedDateBetween(addedDate, addedDate.plusDays(1));
    }

}
