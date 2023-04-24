package uz.md.synccachereactive.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.entity.Range;

import java.time.LocalDateTime;
import java.util.List;

public interface CacheRangeService {

    Mono<Boolean> isEmpty();

    Mono<Boolean> isEmpty(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo);

    Flux<String> findAllCards();

    Mono<RangeDTO> getCacheRangeByCardNumber(String cardNumber);

    Mono<Void> setRange(Range range);

    Mono<Void> deleteAllRanges();

    Mono<Boolean> existsCacheRangeByCardNumber(String cardNumber);

    Mono<Void> setRanges(List<Range> ranges);
}
