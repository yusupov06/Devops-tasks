package uz.md.synccachereactive.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Range;
import uz.md.synccachereactive.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface GetFromCacheService {

    Mono<Map<String, List<TransactionDTO>>> getAllFromCache(List<String> cachedCards, GetByDatesRequest request);

    Flux<Transaction> getFromCache(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate);

    Flux<Transaction> setRangeAndGetFromClient(RangeDTO rangeDTO, String cardNumber, LocalDateTime fromDate, LocalDateTime toDate);

    Flux<Transaction> getFromCacheWithRange(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate, Range range);

    Mono<Map<String, List<Transaction>>> getAsMapBetween(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate);
}
