package uz.md.synccachereactive.service;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import uz.md.synccachereactive.repository.RangeRepository;
import uz.md.synccachereactive.repository.TransactionRepository;
import uz.md.synccachereactive.util.TestUtil;
import uz.md.synccachereactive.utils.AppUtils;
import uz.md.synccachereactive.utils.MockGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;
import static uz.md.synccachereactive.util.TestUtil.transactionsAndDTOsEquals;
import static uz.md.synccachereactive.util.TestUtil.transactionsEquals;

@SpringBootTest
@ActiveProfiles("test")
public class UzCardTransactionsTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private UzCardClient uzCardClient;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheRangeService cacheRangeService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RangeRepository rangeRepository;

    @BeforeEach
    void init() {
        // clear the cache
        cacheService.invalidateAll().block();
        cacheRangeService.deleteAllRanges().block();
    }

    /**
     * Should get from client with list of cards
     */
    @Test
    void shouldGetWithListOfCardsFromClientService() {

        List<String> cards = MockGenerator.getUzCards().subList(0, 3);
        LocalDateTime fromDate = LocalDateTime.of(2023,3,10,15,10,12,0);
        LocalDateTime toDate = LocalDateTime.of(2023,3,14,15,10,10,0);
        GetByDatesRequest request = new GetByDatesRequest(cards, fromDate, toDate);

        setThisRequestAndCheckIt(request);

    }

    /**
     * Should get from cache after cached
     */
    @Test
    void shouldGetFromCache() {

        List<String> cards = MockGenerator.getUzCards().subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.of(2023,3,9,10,10,10,0);
        LocalDateTime toDate = LocalDateTime.of(2023,3,15,10,10,10,0);
        GetByDatesRequest request = new GetByDatesRequest(cards, fromDate, toDate);

        setThisRequestAndCheckIt(request);

        GetByDatesRequest request2 = new GetByDatesRequest(cards, fromDate, toDate);

        ResponseEntity<Map<String, List<TransactionDTO>>> response = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call to client
        callToClient(request);

        Assertions.assertNotNull(response);
        Map<String, List<TransactionDTO>> body = response.getBody();
        Assertions.assertNotNull(body);


        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }

    /**
     * should get from cache and client
     */
    @Test
    void shouldGetFromCacheAndClient() {

        List<String> cards = MockGenerator.getUzCards().subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.of(2023,3,10,10,10,10,0);
        LocalDateTime toDate = LocalDateTime.of(2023,3,15,10,10,10,0);
        GetByDatesRequest request = new GetByDatesRequest(cards, fromDate, toDate);

        // first call
        setThisRequestAndCheckIt(request);

        String card = MockGenerator.getUzCards().get(4);
        cards.add(card);

        GetByDatesRequest request2 = new GetByDatesRequest(cards, fromDate, toDate);

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(fromDate.toLocalDate(), toDate.toLocalDate());

        List<Transaction> transactions = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(AppUtils.cardPredicate(card).and(datePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(card, fromDate.toLocalDate(), toDate.toLocalDate()))
                .thenReturn(Flux.fromIterable(transactions));

        ResponseEntity<Map<String, List<TransactionDTO>>> response = transactionService
                .getByDateBetween(request2)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(1), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(2), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Assertions.assertNotNull(response);
        Map<String, List<TransactionDTO>> body = response.getBody();
        Assertions.assertNotNull(body);

        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }

    void setThisRequestAndCheckIt(GetByDatesRequest request) {
        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(request.getDateFrom().toLocalDate(),
                        request.getDateTo().toLocalDate());

        List<List<Transaction>> mockUzCardTransactions = TestUtil
                .mockUzCardTransactions(cardPredicates, datePredicate);

        // check for cache is empty
        isCacheEmpty(request);

        // When transaction
        whenMockReturn(request, mockUzCardTransactions);

        // First call and save to cache
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request)
                .block();

        // Verify that call to client
        callToClient(request);

        // Check for cached
        checkForCorrectlyCached(request);

        Assertions.assertNotNull(responseEntity);
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();
        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }

    private void checkForCorrectlyCached(GetByDatesRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            checkForCorrectlyCached(cardNumber, request.getDateFrom(), request.getDateTo());
        }
    }

    private void callToClient(GetByDatesRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            // Check for call to client
            Mockito.verify(uzCardClient, Mockito.times(1))
                    .getTransactionsBetweenDates(cardNumber, request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());
        }
    }

    private void whenMockReturn(GetByDatesRequest request, List<List<Transaction>> transactions) {
        int k = 0;
        for (String cardNumber : request.getCardNumbers()) {
            when(uzCardClient.getTransactionsBetweenDates(cardNumber, request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate()))
                    .thenReturn(Flux.fromIterable(transactions.get(k++)));
        }
    }

    private void isCacheEmpty(GetByDatesRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            Assertions.assertEquals(Boolean.TRUE, cacheRangeService.isEmpty(cardNumber, request.getDateFrom(), request.getDateTo())
                    .block());
        }
    }

    private void checkForCorrectlyCached(String card, LocalDateTime dateFrom, LocalDateTime dateTo) {


        // check for response is cached
        Assertions.assertNotEquals(Boolean.TRUE, cacheRangeService
                .isEmpty(card, dateFrom, dateTo)
                .block());

        // check for correctly cached
        List<Transaction> fromCache = cacheService
                .getAllBetween(card, dateFrom, dateTo)
                .collectList()
                .block();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom,
                        dateTo);

        List<Transaction> mockUzCardTransactionsAfterCall = MockGenerator
                .getUzCardTransactions().stream()
                .filter(AppUtils.cardPredicate(card).and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        Assertions.assertNotNull(fromCache);
        transactionsEquals(fromCache, mockUzCardTransactionsAfterCall);

        RangeDTO cacheRange = cacheRangeService.getCacheRangeByCardNumber(card)
                .block();

        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(card, cacheRange.getCardNumber());
//        Assertions.assertEquals(dateFrom, cacheRange.getFromDate());
//        Assertions.assertEquals(dateTo, cacheRange.getToDate());

    }


}
