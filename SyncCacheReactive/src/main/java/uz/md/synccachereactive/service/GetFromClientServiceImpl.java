package uz.md.synccachereactive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.cache.CacheRangeService;
import uz.md.synccachereactive.cache.CacheService;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Range;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.mapper.TransactionMapper;
import uz.md.synccachereactive.strategy.GetTransactionsComposite;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetFromClientServiceImpl implements GetFromClientService {

    private final GetTransactionsComposite transactionsComposite;
    private final TransactionMapper transactionMapper;
    private final CacheRangeService cacheRangeService;
    private final CacheService cacheService;

    /**
     * Get With all not cached cards from client
     *
     * @param notCachedCards List of cards that is not existed in cache
     * @param request        our request
     * @return Mono Map String - card List of TransactionDTO for that card
     */
    @Override
    public Mono<Map<String, List<TransactionDTO>>> getAllFromClient(List<String> notCachedCards, GetByDatesRequest request) {

        // Request validation
        if (notCachedCards == null
                || request == null
                || request.getCardNumbers() == null
                || request.getDateFrom() == null
                || request.getDateTo() == null)
            return Mono.error(() -> new BadRequestException("Bad request"));

        Map<String, List<TransactionDTO>> responseMap = new HashMap<>();
        return transactionsComposite.getTransactionsBetweenDays(notCachedCards, request.getDateFrom(), request.getDateTo())
                .filter(Objects::nonNull)
                .log("Not cached transactions")
                .flatMap(stringListMap -> {
                    List<Range> ranges = new ArrayList<>();
                    List<Transaction> transactions = new ArrayList<>();
                    for (Map.Entry<String, List<Transaction>> entry : stringListMap.entrySet()) {
                        String card = entry.getKey();
                        List<Transaction> list = entry.getValue();
                        list.sort(Comparator.comparing(Transaction::getAddedDate));
                        if (card != null) {
                            responseMap.put(card, transactionMapper.toDTO(list));
                            ranges.add(Range.builder()
                                    .fromDate(request.getDateFrom())
                                    .toDate(request.getDateTo())
                                    .cardNumber(card)
                                    .build());
                            transactions.addAll(list);
                        }
                    }
                    Mono<Void> setRanges = cacheRangeService.setRanges(ranges);
                    log.info("All ranges: " + ranges.size());
                    Mono<Void> putTransactions = cacheService.putAll(transactions);
                    log.info("All transaction: " + transactions.size());
                    return setRanges
                            .then(putTransactions)
                            .thenReturn(responseMap);
                });
    }
}
