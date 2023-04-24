package uz.md.synccachereactive.strategy;

import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface GetTransactionsStrategy {

    String getCardPrefix();

    Mono<Map<String, List<Transaction>>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo);
}
