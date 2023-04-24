package uz.md.synccachereactive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.cache.CacheRangeService;
import uz.md.synccachereactive.cache.CacheService;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Range;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.exceptions.BadRequestException;
import uz.md.synccachereactive.mapper.TransactionMapper;
import uz.md.synccachereactive.strategy.GetTransactionsComposite;
import uz.md.synccachereactive.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GetFromCacheServiceImpl implements GetFromCacheService {

    private final CacheRangeService cacheRangeService;
    private final CacheService cacheService;
    private final GetTransactionsComposite transactionsComposite;
    private final TransactionMapper transactionMapper;

    @Override
    public Mono<Map<String, List<TransactionDTO>>> getAllFromCache(List<String> cachedCards, GetByDatesRequest request) {

        log.info("Get all from cache with list of cards {}, {}", cachedCards, request);

        // Request validation
        if (cachedCards == null
                || request == null
                || request.getDateFrom() == null
                || request.getDateTo() == null)
            return Mono.error(() -> new BadRequestException("Bad request"));

        Map<String, List<TransactionDTO>> responseMap = new HashMap<>();

        return Flux.fromIterable(cachedCards)
                .flatMap(card -> getFromCache(card, request.getDateFrom(), request.getDateTo())
                        .log("Get from cache")
                        .collectList()
                        .map(transactions -> {
                            List<Transaction> afterFilter = transactions
                                    .stream()
                                    .filter(AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo()))
                                    .sorted(Comparator.comparing(Transaction::getAddedDate))
                                    .toList();

                            log.info("Get all from cache {}", transactions);
                            log.info("After filter {}", afterFilter);
                            responseMap.put(card, transactionMapper.toDTO(transactions));
                            return Mono.empty();
                        })
                )
                .then()
                .thenReturn(responseMap);
    }


    /**
     * If range is existed in cache with card number
     * <p>Our card is 8600 and this card range is existed [1-4] [5-9]</p>
     * Even our request is [11-12]
     *
     * @param cardNumber we wanted card
     * @param fromDate   our request from date
     * @param toDate     our request to date
     * @return Flux of Transaction
     */
    @Override
    public Flux<Transaction> getFromCache(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate) {

        log.info("Getting from cache with card number: " + cardNumber + ", from date: " + fromDate + ", to date: " + toDate);

        return cacheRangeService.getCacheRangeByCardNumber(cardNumber)
                .filter(rangeDTO -> rangeDTO != null && rangeDTO.getFromDate() != null && rangeDTO.getToDate() != null)
                .log("Filter completed")
                .flatMap(rangeDTO -> cacheService.getAllBetween(cardNumber, fromDate, toDate)
                        .collectList()
                        .log("Getting from cache")
                        .flatMap(fromCache -> {
                            if (fromCache.isEmpty()) {
                                log.info("Getting from client with request " + cardNumber + " " + fromDate + "" + toDate);
                                return setRangeAndGetFromClient(rangeDTO, cardNumber, fromDate, toDate)
                                        .collectList();
                            } else {
                                Range range = Range.builder()
                                        .fromDate(rangeDTO.getFromDate())
                                        .toDate(rangeDTO.getToDate())
                                        .cardNumber(cardNumber)
                                        .build();

                                return getFromCacheWithRange(cardNumber, fromDate, toDate, range)
                                        .filter(Objects::nonNull)
                                        .collectList()
                                        .flatMap(transactions -> {
                                            transactions.addAll(fromCache);
                                            return cacheService
                                                    .putAll(transactions)
                                                    .thenReturn(transactions);
                                        });
                            }
                        })
                )
                .log("From cache returned")
                .doOnNext(transactions -> log.info("Transactions {}", transactions))
                .flatMapMany(Flux::fromIterable);
    }


    /**
     * If cache is empty with card and our request dates
     * When cache range is [1-5] but our request [9-11]
     * it cached get transactions and save cache range
     *
     * @param rangeDTO   range of card
     * @param cardNumber card we want to get transactions
     * @param fromDate   our request from date
     * @param toDate     our request to date
     * @return Flux of Transaction from client
     */
    @Override
    public Flux<Transaction> setRangeAndGetFromClient(RangeDTO rangeDTO, String cardNumber, LocalDateTime fromDate, LocalDateTime toDate) {

        log.info("If cache empty call to client with " + cardNumber + " dates: " + fromDate + " " + toDate);

        if (rangeDTO == null || cardNumber == null || fromDate == null || toDate == null)
            return Flux.error(new BadRequestException("Bad request"));

        Range range = Range.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .cardNumber(cardNumber)
                .build();

        // we have to set fromDate
        // if we after cached transactions we have to know fromDate
        if (rangeDTO.getToDate() != null
                && rangeDTO.getToDate().isBefore(range.getFromDate())) {
            range.setFromDate(rangeDTO.getToDate());
        }

        // we have to set toDate
        // if we after cached transactions we have to know toDate
        if (rangeDTO.getFromDate() != null
                && rangeDTO.getFromDate().isAfter(range.getToDate())) {
            range.setToDate(rangeDTO.getFromDate());
        }

        log.info("Range is  " + range);

        // Get transactions from client
        return transactionsComposite
                .getTransactionsBetweenDays(List.of(cardNumber), range.getFromDate(), range.getToDate())
                .filter(stringListMap -> stringListMap != null && stringListMap.containsKey(cardNumber))
                .log("Get transactions from client")
                .map(stringListMap -> stringListMap.get(cardNumber))
                .filter(Objects::nonNull)
                .log("After getting from client")
                .flatMap(transactions -> cacheService
                        .putAll(transactions)
                        .then(cacheRangeService.setRange(range))
                        .thenReturn(transactions))
                .flatMapMany(Flux::fromIterable);
    }


    /**
     * If cache is not empty with card fromDate and toDate
     * When our card is cached before
     * And our request is around cache range
     * <b>Examples</b>
     * <p>request [1-5] cache [1-3]</p>
     * <p>request [1-8] cache [2-4]</p>
     * <p>request [1-4] cache [3-6]</p>
     * <p>request [5-8] cache [3-6]</p>
     *
     * @param cardNumber wanted card
     * @param fromDate   our request from date
     * @param toDate     our request to date
     * @param range      card range
     * @return Flux of Transaction
     */
    @Override
    public Flux<Transaction> getFromCacheWithRange(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate, Range range) {

        log.info("Get from cache with range " + fromDate + " " +  toDate);

        if (cardNumber == null || fromDate == null || toDate == null || range == null) {
            return Flux.error(new BadRequestException("Bad request"));
        }

        log.info("Get from cache with range " + cardNumber + " and " + fromDate + " : " + toDate);
        log.info("Range: " + range);

        // If our request is getting [1-10] range but our cache range is [4-7]
        // we get from client with two calls [1-3] and [8-10]

        if (fromDate.isBefore(range.getFromDate())
                && toDate.isAfter(range.getToDate())) {

            log.info("When request is [1-6] range is [3-4]");
            Mono<List<Transaction>> fromClient1 = getAsMapBetween(cardNumber, fromDate, range.getFromDate().minusNanos(1))
                    .filter(responseMap -> responseMap != null && responseMap.size() > 0 && responseMap.containsKey(cardNumber))
                    .log("From cache 1")
                    .map(responseMap -> {
                        range.setFromDate(fromDate);
                        return responseMap.get(cardNumber);
                    });

            Mono<List<Transaction>> fromClient2 = getAsMapBetween(cardNumber, range.getToDate().plusNanos(1), toDate)
                    .filter(responseMap -> responseMap != null && responseMap.size() > 0 && responseMap.containsKey(cardNumber))
                    .log("From cache 2")
                    .map(responseMap -> {
                        range.setToDate(toDate);
                        return responseMap.get(cardNumber);
                    });

            return fromClient1.zipWith(fromClient2)
                    .filter(Objects::nonNull)
                    .flatMap(objects -> {
                        List<Transaction> t1 = objects.getT1();
                        List<Transaction> t2 = objects.getT2();
                        List<Transaction> all = new ArrayList<>();
                        all.addAll(t1);
                        all.addAll(t2);
                        return cacheRangeService
                                .setRange(range)
                                .thenReturn(all);
                    })
                    .flatMapMany(Flux::fromIterable);
        } else if (fromDate.isBefore(range.getFromDate())) {

            log.info(fromDate + " and " + range.getFromDate());
            log.info("Equality : " + fromDate.equals(range.getFromDate()));

            // If our request is getting [1-5] range but our cache range is [4-5] or [4-6]
            // we get from client with a call [1-3]

            log.info("When request is [1-8] range is [3-8]");

            return getAsMapBetween(cardNumber, fromDate, range.getFromDate().minusNanos(1))
                    .filter(responseMap -> responseMap != null && responseMap.size() > 0 && responseMap.containsKey(cardNumber))
                    .map(responseMap -> {
                        range.setFromDate(fromDate);
                        return responseMap.get(cardNumber);
                    })
                    .flatMap(transactions -> cacheRangeService
                            .setRange(range)
                            .thenReturn(transactions))
                    .flatMapMany(Flux::fromIterable);

        } else if (toDate.isAfter(range.getToDate())) {

            // If our request is getting [2-8] range but our cache range is [1-5] or [2-5]
            // we get from client with a call [6-8]

            log.info("When request is [1-8] range is [1-5]");

            return getAsMapBetween(cardNumber, range.getToDate().plusNanos(1), toDate)
                    .filter(responseMap -> responseMap != null && responseMap.size() > 0 && responseMap.containsKey(cardNumber))
                    .map(responseMap -> {
                        range.setToDate(toDate);
                        return responseMap.get(cardNumber);
                    })
                    .flatMap(transactions -> cacheRangeService
                            .setRange(range)
                            .thenReturn(transactions))
                    .flatMapMany(Flux::fromIterable);
        } else {
            log.info("request is equals range");
            return Flux.fromIterable(new ArrayList<>());
        }

    }


    /**
     * Call to client and return as Map
     *
     * @param cardNumber requested card
     * @param fromDate   requested fromDate
     * @param toDate     requested toDate
     * @return Mono Map
     */
    @Override
    public Mono<Map<String, List<Transaction>>> getAsMapBetween(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate) {

        Map<String, List<Transaction>> response = new HashMap<>();

        return transactionsComposite
                .getTransactionsBetweenDays(List.of(cardNumber), fromDate, toDate)
                .log("Get from cache range")
                .flatMapMany(transactionsMap -> Flux.fromIterable(transactionsMap.entrySet()))
                .filter(Objects::nonNull)
                .doOnNext(entry -> {
                    String cardNum = entry.getKey();
                    List<Transaction> transactions = entry.getValue();
                    response.put(cardNum, transactions);
                })
                .then()
                .thenReturn(response);

    }


}
