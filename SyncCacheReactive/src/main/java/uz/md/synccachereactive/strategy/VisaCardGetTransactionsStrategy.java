package uz.md.synccachereactive.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.clientService.VisaCardClient;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.utils.AppUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class VisaCardGetTransactionsStrategy implements GetTransactionsStrategy {

    private final VisaCardClient visaCardClient;

    @Override
    public String getCardPrefix() {
        return "5555";
    }

    @Override
    public Mono<Map<String, List<Transaction>>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {

        if (cards == null || dateFrom == null || dateTo == null)
            return Mono.error(() -> new BadRequestException("Invalid request"));

        LocalDate fromDate = dateFrom.toLocalDate();
        LocalDate toDate = dateTo.toLocalDate();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom, dateTo);

        return Flux.fromIterable(cards)
                .flatMap(card -> visaCardClient.getTransactionsBetweenDates(card, fromDate, toDate)
                        .filter(dateTimePredicate)
                        .collectList()
                        .map(transactionList -> new AbstractMap.SimpleEntry<>(card, transactionList))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
