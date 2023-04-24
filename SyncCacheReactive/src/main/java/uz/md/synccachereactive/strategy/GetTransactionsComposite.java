package uz.md.synccachereactive.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.exceptions.NotFoundException;
import uz.md.synccachereactive.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetTransactionsComposite implements GetTransactionsStrategy {

    private final ApplicationContext context;

    private final Map<String, GetTransactionsStrategy> strategyMap = new HashMap<>();

    @Override
    public String getCardPrefix() {
        return strategyMap.values().toString();
    }

    @Override
    public Mono<Map<String, List<Transaction>>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {

        log.info("getTransactionsBetweenDays called with " + cards + ", " + dateFrom + ", " + dateTo);

        if (cards == null || dateFrom == null || dateTo == null)
            return Mono.error(() -> new BadRequestException("Bad request"));

        Map<String, List<Transaction>> response = new HashMap<>();
        Map<String, List<String>> cardsGroup = AppUtils.getCardsGroup(cards);

        return Flux.fromIterable(cardsGroup.keySet())
                .log("Cards iterating in composite : " + cards)
                .flatMap(cardPrefix -> {
                    log.info("Card prefix: " + cardPrefix);
                    GetTransactionsStrategy strategy = getStrategy(cardPrefix);
                    return strategy.getTransactionsBetweenDays(cardsGroup.get(cardPrefix), dateFrom, dateTo)
                            .doOnNext(stringListMap -> {
                                for (Map.Entry<String, List<Transaction>> entry : stringListMap.entrySet()) {
                                    log.info("Entry {} : {}", entry.getKey(), entry.getValue().size());
                                }
                            })
                            .doOnNext(response::putAll)
                            .then();
                })
                .then(Mono.just(response))
                .doOnNext(stringListMap -> log.info("stringListMap = {}", stringListMap));

    }

    private GetTransactionsStrategy getStrategy(String cardPrefix) {

        GetTransactionsStrategy strategy = strategyMap.get(cardPrefix);

        if (strategy == null) {

            // Get all the beans of type GetTransactionsStrategy
            Map<String, GetTransactionsStrategy> strategyBeans = context
                    .getBeansOfType(GetTransactionsStrategy.class);

            // Find the strategy bean that matches the card prefix
            strategy = strategyBeans.values()
                    .stream()
                    .filter(s -> s.getCardPrefix().equals(cardPrefix))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("No strategy found for card prefix: " + cardPrefix));

            // Add the strategy to the map for future lookups
            addStrategy(strategy);
        }

        return strategy;
    }

    public void addStrategy(GetTransactionsStrategy strategy) {
        strategyMap.put(strategy.getCardPrefix(), strategy);
    }

    public void removeStrategy(GetTransactionsStrategy strategy) {
        strategyMap.remove(strategy.getCardPrefix());
    }

    public void clearAll() {
        strategyMap.clear();
    }


}
