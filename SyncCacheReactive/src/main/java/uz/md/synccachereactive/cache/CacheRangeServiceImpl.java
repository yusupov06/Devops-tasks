package uz.md.synccachereactive.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.entity.Range;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.mapper.RangeMapper;
import uz.md.synccachereactive.repository.RangeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheRangeServiceImpl implements CacheRangeService {

    private final RangeRepository rangeRepository;
    private final RangeMapper rangeMapper;

    @Override
    public Mono<Boolean> isEmpty() {
        log.info("is empty");
        return rangeRepository
                .count()
                .flatMap(count -> Mono.just(count == null || count == 0));
    }

    @Override
    public Mono<Boolean> isEmpty(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo) {

        if (cardNumber == null || dateFrom == null || dateTo == null)
            return Mono.error(() -> new BadRequestException("Request is invalid"));

        return rangeRepository
                .existsByCardNumber(cardNumber)
                .flatMap(existed -> Mono.just(!existed));
    }

    @Override
    public Flux<String> findAllCards() {
        return rangeRepository.findAll()
                .flatMap(card -> Flux.just(card.getCardNumber()));
    }

    @Override
    public Mono<RangeDTO> getCacheRangeByCardNumber(String cardNumber) {
        log.info("Getting cache range for card " + cardNumber);
        if (cardNumber == null)
            return Mono.error(() -> new BadRequestException("Invalid card number"));
        return rangeRepository
                .findByCardNumber(cardNumber)
                .flatMap(range -> Mono.just(rangeMapper.toDTO(range)));
    }

    @Override
    public Mono<Void> setRange(Range range) {
        if (range.getCardNumber() == null
                || range.getFromDate() == null
                || range.getToDate() == null)
            return Mono.error(() -> new BadRequestException("Invalid request"));

        return rangeRepository.existsByCardNumber(range.getCardNumber())
                .log("Cache range saving")
                .flatMap(existed -> {
                    if (existed)
                        return rangeRepository
                                .deleteByCardNumber(range.getCardNumber())
                                .log("Deleting cache range")
                                .then(rangeRepository.save(range));
                    log.info("range : " + range);
                    return rangeRepository.save(range);
                })
                .log("Cache range successfully saved")
                .then();
    }

    @Override
    public Mono<Void> deleteAllRanges() {
        return rangeRepository.deleteAll();
    }

    @Override
    public Mono<Boolean> existsCacheRangeByCardNumber(String cardNumber) {
        log.info("Exists cache range by card number: {}", cardNumber);
        return rangeRepository.existsByCardNumber(cardNumber);
    }

    @Override
    public Mono<Void> setRanges(List<Range> ranges) {
        return Flux.fromIterable(ranges)
                .flatMap(this::setRange)
                .then();
    }
}
