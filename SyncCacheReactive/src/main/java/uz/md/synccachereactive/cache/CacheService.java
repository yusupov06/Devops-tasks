package uz.md.synccachereactive.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface CacheService {

    Mono<Void> put(Transaction value);

    Mono<Void> putAll(List<Transaction> list);

    Flux<Transaction> getAllBetween(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo);

    Mono<Void> invalidateAll();

}
