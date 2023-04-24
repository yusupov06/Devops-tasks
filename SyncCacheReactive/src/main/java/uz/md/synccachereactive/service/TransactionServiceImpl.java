package uz.md.synccachereactive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.cache.CacheRangeService;
import uz.md.synccachereactive.cache.CacheService;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.mapper.TransactionMapper;
import uz.md.synccachereactive.strategy.GetTransactionsComposite;
import uz.md.synccachereactive.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final CacheService cacheService;
    private final CacheRangeService cacheRangeService;
    private final GetFromCacheService getFromCacheService;
    private final GetFromClientService getFromClientService;
    private final GetTransactionsComposite transactionsComposite;
    private final TransactionMapper transactionMapper;

    /**
     * Main method every call is in
     * @param request wanted request
     * @return Mono Map     *  -> card and its List of transactions
     */
    @Override
    public Mono<ResponseEntity<Map<String, List<TransactionDTO>>>> getByDateBetween(GetByDatesRequest request) {

        log.info("Getting by date between ");

        // Request validation
        if (request == null
                || request.getDateTo() == null
                || request.getDateFrom() == null
                || request.getCardNumbers() == null) {
            return Mono.error(() -> new BadRequestException("Request cannot be null"));
        }

        // Swap if fromDate > toDate
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            log.info("Request dates swap");
            LocalDateTime dateFrom = request.getDateFrom();
            request.setDateFrom(request.getDateTo());
            request.setDateTo(dateFrom);
        }

        log.info("Cards {}", request.getCardNumbers());

        List<String> notCached = new ArrayList<>();
        Map<String, List<TransactionDTO>> response = new HashMap<>();

        return Flux.fromIterable(request.getCardNumbers())
                .log("Cards iterating")
                .flatMap(card -> cacheRangeService.existsCacheRangeByCardNumber(card)
                        .log("Card checking for existed in cache")
                        .flatMap(exists -> {
                            if (exists) {
                                return getFromCacheService.getFromCache(card, request.getDateFrom(), request.getDateTo())
                                        .log("Get from cache")
                                        .collectList()
                                        .map(transactions -> {
                                            List<Transaction> afterFilter = transactions
                                                    .stream()
                                                    .filter(AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo()))
                                                    .sorted(Comparator.comparing(Transaction::getAddedDate))
                                                    .toList();

                                            log.info("Get all from cache {}", transactions.size());
                                            log.info("After filter {}", afterFilter.size());
                                            response.put(card, transactionMapper.toDTO(afterFilter));
                                            return Mono.empty();
                                        });
                            } else {
                                notCached.add(card);
                                log.info("cards that not cached: {}", notCached);
                            }
                            return Mono.empty();
                        }))
                .then()
                .log("After cards iterating cards not cached get from client")
                .thenReturn(response)
                .flatMap(responseMap -> getFromClientService
                        .getAllFromClient(notCached, request)
                        .map(map -> {
                            responseMap.putAll(map);
                            return responseMap;
                        }))
                .doOnCancel(() -> log.info("Not cached transactions canceled"))
                .log("Returning result")
                .map(responseMap -> {
                    log.info("Response map: " + responseMap);
                    return new ResponseEntity<>(responseMap, HttpStatus.OK);
                });
    }


    /**
     * Job that run in job check for cached data and update all
     * @return Mono Void
     */
    @Override
    public Mono<Void> checkForCachedDataAndUpdate() {

        log.info("Checking for cached data and update");

        return cacheRangeService.isEmpty()
                .flatMap(isEmpty -> {
                    if (isEmpty) {
                        log.info("No data in cache");
                        return Mono.empty();
                    } else {
                        // Get cards that we cached
                        return cacheRangeService.findAllCards()
                                .flatMap(card -> cacheRangeService
                                        .getCacheRangeByCardNumber(card)
                                        .flatMap(cacheRange -> {
                                            if (cacheRange != null && cacheRange.getFromDate() != null && cacheRange.getToDate() != null) {
                                                return transactionsComposite
                                                        .getTransactionsBetweenDays(List.of(card), cacheRange.getFromDate(), cacheRange.getToDate())
                                                        .filter(listMap -> listMap != null && !listMap.isEmpty())
                                                        .flatMap(listMap -> cacheService.putAll(listMap.get(card)))
                                                        .then();
                                            }
                                            return Mono.empty();
                                        })
                                )
                                .then();
                    }
                });

    }

}
