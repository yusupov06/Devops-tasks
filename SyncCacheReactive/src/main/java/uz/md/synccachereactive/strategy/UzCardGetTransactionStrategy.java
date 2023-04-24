package uz.md.synccachereactive.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.clientService.UzCardClient;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.utils.AppUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
@Slf4j
public class UzCardGetTransactionStrategy implements GetTransactionsStrategy {

    private final UzCardClient uzCardClient;

    @Override
    public String getCardPrefix() {
        return "8600";
    }

    @Override
    public Mono<Map<String, List<Transaction>>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {

        log.info("Getting transactions between days " + dateFrom + " " + dateTo + " in uzcard");

        if (cards == null || dateFrom == null || dateTo == null)
            return Mono.error(() -> new BadRequestException("Invalid request"));

        LocalDate fromDate = dateFrom.toLocalDate();
        LocalDate toDate = dateTo.toLocalDate();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom, dateTo);

        return Flux.fromIterable(cards)
                .flatMap(card -> {
                    log.info("Getting from uzcard with dates " + fromDate + " and " + toDate);
                    return uzCardClient.getTransactionsBetweenDates(card, fromDate, toDate)
                        .filter(dateTimePredicate)
                        .collectList()
                        .map(transactionList -> new AbstractMap.SimpleEntry<>(card, transactionList));
                }
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .doOnNext(stringListMap -> System.out.println("stringListMap = " + stringListMap));
    }

}
