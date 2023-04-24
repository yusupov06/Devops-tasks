package uz.md.synccachereactive.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final TransactionRepository transactionRepository;

    @Override
    public Mono<Void> put(Transaction value) {
        log.info("put to cache " + value);

        if (value == null
                || value.getFromCard() == null
                || value.getToCard() == null
                || value.getAddedDate() == null
                || value.getAmount() == null)
            return Mono.error(() -> new BadRequestException("Invalid Request"));

        return transactionRepository.existsByFromCardAndToCardAndAddedDate(
                        value.getFromCard(),
                        value.getToCard(),
                        value.getAddedDate())
                .flatMap(exists -> {
                    log.info("In transactions exists {}", exists);
                    if (!exists) {
                        log.info("save to db");
                        return transactionRepository.save(value)
                                .then();
                    } else {
                        return transactionRepository.findByFromCardAndToCardAndAddedDate(
                                        value.getFromCard(),
                                        value.getToCard(),
                                        value.getAddedDate()
                                )
                                .flatMap(transaction -> {
                                    value.setId(transaction.getId());
                                    return transactionRepository.save(value)
                                            .then();
                                });
                    }
                });
    }

    @Override
    public Mono<Void> putAll(List<Transaction> list) {
        log.info(" put all to cache " + list);
        HashSet<Transaction> transactions = new HashSet<>(list);
        return Flux.fromIterable(transactions)
                .flatMap(this::put)
                .then();
    }

    @Override
    public Flux<Transaction> getAllBetween(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo) {
        log.info(" get between date " + dateFrom + " and " + dateTo);
        if (cardNumber == null || dateFrom == null || dateTo == null)
            return Flux.error(() -> new BadRequestException("Request is invalid"));
        return transactionRepository
                .findAllByCardAndAddedDateBetween(cardNumber, dateFrom, dateTo)
                .sort(Comparator.comparing(Transaction::getAddedDate));
    }

    @Override
    public Mono<Void> invalidateAll() {
        log.info("invalidating all");
        return transactionRepository.deleteAll();
    }


}
