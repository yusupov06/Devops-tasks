package uz.md.synccachereactive.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import uz.md.synccachereactive.cache.CacheRangeService;
import uz.md.synccachereactive.cache.CacheService;
import uz.md.synccachereactive.clientService.UzCardClient;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.RangeDTO;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.entity.TransactionStatus;
import uz.md.synccachereactive.utils.AppUtils;
import uz.md.synccachereactive.utils.MockGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

import static java.lang.Boolean.TRUE;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uz.md.synccachereactive.util.TestUtil.*;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private UzCardClient uzCardClient;

    @Autowired
    private CacheRangeService cacheRangeService;

    @Autowired
    private CacheService cacheService;

    @BeforeEach
    void init() {
        // clear the cache
        cacheService.invalidateAll()
                .then(cacheRangeService.deleteAllRanges())
                .block();
    }


    /**
     * Should get from uzcard client
     */
    @Test
    void shouldGetFromClientByDateBetween() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService
                .isEmpty()
                .block());

        LocalDateTime fromDate = LocalDateTime.of(2023,3,10,10,10,10,0);
        LocalDateTime toDate = LocalDateTime.of(2023,3,16,10,10,10,0);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(uzCards.subList(0, 1), fromDate, toDate);

        setCallAndTestIt(request);

    }

    /**
     * First call
     *
     * @param request
     */
    private void setCallAndTestIt(GetByDatesRequest request) {

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request.getCardNumbers().get(0));

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(request.getDateFrom().toLocalDate(),
                        request.getDateTo().toLocalDate());

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> mockUzCardTransactions = MockGenerator
                .getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate))
                .toList();

        // check for cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty(request.getCardNumbers().get(0), request.getDateFrom(), request.getDateTo()).block());

        when(uzCardClient.getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactions));

        // First call and save to cache
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response is cached
        Assertions.assertNotEquals(TRUE, cacheRangeService
                .isEmpty(request.getCardNumbers().get(0), request.getDateFrom(), request.getDateTo()).block());

        // check for correctly cached
        List<Transaction> fromCache = cacheService
                .getAllBetween(request.getCardNumbers().get(0), request.getDateFrom(), request.getDateTo())
                .collectList()
                .block();

        List<Transaction> mockUzCardTransactionsAfterCall = mockUzCardTransactions.stream()
                .filter(dateTimePredicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache);
        transactionsEquals(fromCache, mockUzCardTransactionsAfterCall);

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(request.getCardNumbers().get(0)).block();
        Assertions.assertNotNull(cacheRange);

        Assertions.assertNotNull(responseEntity);
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();
        Assertions.assertNotNull(body);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache
     */
    @Test
    void shouldGetFromCacheByDateBetween() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService
                .isEmpty()
                .block());

        LocalDateTime fromDate = LocalDateTime.of(2023, 3, 10, 6, 18, 15, 0);
        LocalDateTime toDate = LocalDateTime.of(2023, 3, 14, 6, 18, 15, 0);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(uzCards.subList(0, 1), fromDate, toDate);

        setCallAndTestIt(request);

        ResponseEntity<Map<String, List<TransactionDTO>>> response = transactionService
                .getByDateBetween(request)
                .block();

        Mockito.verify(uzCardClient, times(1))
                .getTransactionsBetweenDates(uzCards.get(0), fromDate.toLocalDate(), toDate.toLocalDate());

        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(uzCards.get(0));
        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(fromDate, toDate);

        List<Transaction> transactions = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        Assertions.assertNotNull(response);
        Map<String, List<TransactionDTO>> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(1, body.size());
        Assertions.assertTrue(body.containsKey(uzCards.get(0)));
        transactionsAndDTOsEquals(transactions, body.get(uzCards.get(0)));

    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * We called [1-6]
     * And we call [3-5] range this range should be got from cache
     */
    @Test
    void shouldGetFromInsideCache() {
        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService
                .isEmpty()
                .block());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // Second request
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(), LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(3));

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        List<Transaction> mockUzCardTransactionsAfterCall = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();


        Predicate<Transaction> predicate = AppUtils.dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        // After second call result should be like this
        List<Transaction> transactionsAfterSecondCall = mockUzCardTransactionsAfterCall
                .stream()
                .filter(predicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // Second call with [3-5]
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(transactionsAfterSecondCall, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * [5-7] is cached
     * And we call [1-3] range, and [1-4] should be called to client
     */
    @Test
    void shouldGetFromClientWithCachedAnotherRange() {
        // check cache is empty
        Assertions.assertEquals(cacheRangeService
                .isEmpty()
                .block(), TRUE);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(8);
        LocalDateTime toDate = LocalDateTime.now().minusDays(6);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################

        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request.getDateTo().toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request.getDateTo().toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall));

        // Second call with [1-3]
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request.getDateTo().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for cache range set correctly
        RangeDTO cacheRange = cacheRangeService
                .getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();
        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request2.getDateTo());

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> fromCache = cacheService.getAllBetween(request2.getCardNumbers().get(0), request.getDateFrom(), request2.getDateTo())
                .collectList()
                .block();

        // after call result should be like this
        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(AppUtils.cardPredicate(request2.getCardNumbers().get(0)).and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();
        Assertions.assertNotNull(fromCache);
        transactionsEquals(cacheResultShouldBe, fromCache);

        Assertions.assertNotNull(responseEntity);
        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate1 = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        List<Transaction> resultShouldBe = cacheResultShouldBe
                .stream()
                .filter(dateTimePredicate1)
                .toList();

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));
    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [6-8] range this range should be called to client
     * And cache update with range [1-8]
     */
    @Test
    void shouldGetFromClientWithCachedAnotherRange2() {

        // check cache is empty
        Assertions.assertEquals(cacheRangeService.isEmpty().block(), TRUE);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(4);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################

        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.now().minusDays(8),
                LocalDateTime.now().minusDays(6));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall));

        // Second call with [6-8]
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        // Check for range set correctly
        RangeDTO cacheRange = cacheRangeService
                .getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();

        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request2.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());

        // Check for correctly cached transactions

        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request2.getDateFrom(), request.getDateTo())
                .collectList()
                .block();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality

        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);


        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [5-8] is cached
     * And we call [2-7] range <p>[5-7] from cache</p> and [2-4] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty().block());

        // Imagine now 15
        // FromDate = 11
        LocalDateTime fromDate = LocalDateTime.now().minusDays(4);
        // ToDate = 15
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################

        // FromDate = 9
        // ToDate = 13
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now().minusDays(2));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall));

        // Second call
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();
        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request2.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request2.getDateFrom(), request.getDateTo())
                .collectList()
                .block();


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [5-8] is cached
     * And we call [2-7] range [5-7] from cache and [2-4] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClientButClientResponseIsEmpty() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty().block());

        // Imagine now 15
        // FromDate = 11
        LocalDateTime fromDate = LocalDateTime.of(2023, 3, 5, 15, 15, 15, 0);
        // ToDate = 15
        LocalDateTime toDate = LocalDateTime.of(2023, 3, 8, 15, 15, 15, 0);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################

        // FromDate = 9
        // ToDate = 13
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.of(2023, 3, 2, 15, 15, 15, 0),
                LocalDateTime.of(2023, 3, 7, 15, 15, 15, 0));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(Flux.fromIterable(new ArrayList<>()));

        // Second call
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();
        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request2.getDateFrom(), request.getDateTo())
                .collectList()
                .block();


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [2-8] range [2-4] from cache and [5-8] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient2() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty()
                .block());

        // Imagine now 8
        // FromDate = 1
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        // ToDate = 4
        LocalDateTime toDate = LocalDateTime.now().minusDays(4);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################


        // Imagine now is 8
        // FromDate = 2
        // ToDate = 8
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall));

        // Second call
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();

        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request2.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request.getDateFrom(), request2.getDateTo())
                .collectList()
                .block();


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [2-8] range [2-4] from cache and [5-8] from uzcard but result is empty from client
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClientWithEmptyResultFromClient() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty().block());

        // Imagine now 8
        // FromDate = 1
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        // ToDate = 4
        LocalDateTime toDate = LocalDateTime.now().minusDays(4);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################


        // Imagine now is 8
        // FromDate = 2
        // ToDate = 8
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(Flux.fromIterable(new ArrayList<>()));

        // Second call
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(request2.getCardNumbers().get(0))
                .block();

        Assertions.assertNotNull(cacheRange);
//        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
//        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());

        // Check for updated cache
        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request.getDateFrom(), request2.getDateTo())
                .collectList()
                .block();


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate1 = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        List<Transaction> resultShouldBe = cacheResultShouldBe
                .stream()
                .filter(dateTimePredicate1)
                .toList();

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [3-5] is cached
     * And we call [1-8] range [3-5] from cache and [1-2] and [6-8] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient3() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty().block());

        // Imagine now 8
        // FromDate = 3
        LocalDateTime fromDate = LocalDateTime.of(2023,3,10,10,10,0);
        // ToDate = 5
        LocalDateTime toDate = LocalDateTime.of(2023,3,12,10,10,10,0);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // ################### Second request #################

        // Imagine now is 8
        // FromDate = 1
        // ToDate = 8
        GetByDatesRequest request2 = new GetByDatesRequest(request.getCardNumbers(),
                LocalDateTime.of(2023,3,8,10,10,10,0),
                LocalDateTime.of(2023,3,15,10,10,10,0));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumbers().get(0));

        // in Second call with dates

        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().minusNanos(1).toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall1 = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        Predicate<Transaction> datePredicate3 = AppUtils
                .datePredicate(request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall2 = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate3))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().minusNanos(1).toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall1));

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumbers().get(0),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockUzCardTransactionsSecondCall2));

        // Second call
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client 1
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().minusNanos(1).toLocalDate());

        // Check for call to client 2
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumbers().get(0), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // Check for updated cache
        List<Transaction> fromCache2 = cacheService.getAllBetween(request.getCardNumbers().get(0), request2.getDateFrom(), request2.getDateTo())
                .collectList()
                .block();


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache2);
        transactionsEquals(cacheResultShouldBe, fromCache2);


        // check for response
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(cacheResultShouldBe, body.get(request.getCardNumbers().get(0)));

    }

    /**
     * Should get from client if another card cached
     * [1-10] in uzcard client
     * We called [2-6] with one card
     * And we call again this range with another card, and we get from client
     */
    @Test
    void shouldGetFromClientIfAnotherCardCached() {

        // check cache is empty
        Assertions.assertEquals(cacheRangeService.isEmpty().block(), TRUE);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        // Second Call
        // Change card
        request.setCardNumbers(List.of(uzCards.get(1)));

        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty(request.getCardNumbers().get(0), request.getDateFrom(), request.getDateTo())
                .block());

        RangeDTO cacheRange2 = cacheRangeService.getCacheRangeByCardNumber(request.getCardNumbers().get(0))
                .block();
        Assertions.assertNull(cacheRange2);

        setCallAndTestIt(request);

    }

    /**
     * Should update cached transactions
     */
    @Test
    void shouldUpdateCachedTransactions() {

        // check cache is empty
        Assertions.assertEquals(TRUE, cacheRangeService.isEmpty().block());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();

        GetByDatesRequest request = new GetByDatesRequest(List.of(uzCards.get(0)), fromDate, toDate);

        setCallAndTestIt(request);

        List<Transaction> fromCache1 = Objects.requireNonNull(cacheService.getAllBetween(request.getCardNumbers().get(0), request.getDateFrom(), request.getDateTo())
                        .collectList()
                        .block())
                .stream()
                .map(transaction -> Transaction.builder()
                        .id(transaction.getId())
                        .addedDate(transaction.getAddedDate())
                        .fromCard(transaction.getFromCard())
                        .toCard(transaction.getToCard())
                        .status(transaction.getStatus())
                        .amount(transaction.getAmount())
                        .build())
                .toList();

        String card = request.getCardNumbers().get(0);
        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(card)
                .block();

        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(card);
        Assertions.assertNotNull(cacheRange);
        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(cacheRange.getFromDate(), cacheRange.getToDate());

        List<Transaction> mockTransactions = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .map(transaction -> Transaction.builder()
                        .amount(transaction.getAmount().add(BigDecimal.ONE))
                        .status(TransactionStatus.FAILED)
                        .fromCard(transaction.getFromCard())
                        .toCard(transaction.getToCard())
                        .addedDate(transaction.getAddedDate())
                        .build())
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(card, cacheRange.getFromDate().toLocalDate(), cacheRange.getToDate().toLocalDate()))
                .thenReturn(Flux.fromIterable(mockTransactions));

        transactionService.checkForCachedDataAndUpdate()
                .block();

        Mockito.verify(uzCardClient, times(2))
                .getTransactionsBetweenDates(card, cacheRange.getFromDate().toLocalDate(), cacheRange.getToDate().toLocalDate());

        List<Transaction> transactions = cacheService.getAllBetween(request.getCardNumbers().get(0), cacheRange.getFromDate(), cacheRange.getToDate())
                .collectList()
                .block();
        Assertions.assertNotNull(transactions);
        transactionAfterUpdateNotEquals(fromCache1, transactions);

    }


}
