package uz.md.synccache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.clientService.ClientService;
import uz.md.synccache.config.MyCache;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.service.TransactionService;
import uz.md.synccache.util.Mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private ClientService clientService;

    @Autowired
    private MyCache myCache;

    @BeforeEach
    void init() {
        // clear the cache
        myCache.invalidateAll();
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetween() {
        GetByDateRequest request = new GetByDateRequest(null, LocalDate.now());
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        // Check for not to call to client
        Mockito.verify(clientService, Mockito.times(0))
                .getAllByDateBetween(request.getDateFrom(), request.getDateTo());

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetween2() {
        GetByDateRequest request = new GetByDateRequest(LocalDate.now().minusDays(5), null);
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        // Check for not to call to client
        Mockito.verify(clientService, Mockito.times(0))
                .getByDate(ArgumentMatchers.any());

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetween3() {
        GetByDateRequest request = new GetByDateRequest(null, null);
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        // Check for not to call to client
        Mockito.verify(clientService, Mockito.times(0))
                .getByDate(ArgumentMatchers.any());

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetween4() {
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(null));

        // Check for not to call to client
        Mockito.verify(clientService, Mockito.times(0))
                .getByDate(ArgumentMatchers.any());

        Assertions.assertTrue(myCache.isEmpty());
    }

    /**
     * [ 1- 10] range in client service we want get
     * [1-3] range, and we get from client
     */
    @Test
    void shouldGetFromClient() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDate fromDate = LocalDate.now().minusDays(5);
        LocalDate toDate = LocalDate.now();

        GetByDateRequest request = new GetByDateRequest(fromDate, toDate);

        // check for cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Mock response when to call client
        List<Transaction> mockTransactions = Mock
                .generateTransactions(10, 1L, request.getDateFrom(), request.getDateTo());

        when(clientService.getAllByDateBetween(request.getDateFrom(), request.getDateTo()))
                .thenReturn(mockTransactions);

        // Response
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(request.getDateFrom(), request.getDateTo());

        // check for correctly cached
        List<Transaction> fromCache = myCache.getAllBetween(request.getDateFrom(), request.getDateTo());
        mockTransactions.sort(Comparator.comparing(Transaction::getAddedDate));
        transactionsEquals(fromCache, mockTransactions);

        Assertions.assertNotNull(responseEntity);
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        List<TransactionDTO> body = responseEntity.getBody();

        transactionsAndDTOsEquals(mockTransactions, body);

    }

    /**
     * [ 1- 10] range in client service we want get
     * [1-3] range, and we get from client but with another range cache contains transactions
     */
    @Test
    void shouldGetFromClient2() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDate fromDate = LocalDate.now().minusDays(5);
        LocalDate toDate = LocalDate.now();

        GetByDateRequest request = new GetByDateRequest(fromDate, toDate);

        // Mock response when to call client
        List<Transaction> mockTransactions = Mock
                .generateTransactions(10, 1L, request.getDateFrom(), request.getDateTo());

        when(clientService.getAllByDateBetween(request.getDateFrom(), request.getDateTo()))
                .thenReturn(mockTransactions);

        LocalDate fromDateCached = LocalDate.now().minusDays(10);
        LocalDate toDateCached = LocalDate.now().minusDays(6);
        List<Transaction> mockTransactionsForCache = Mock
                .generateTransactions(10, 1L, fromDateCached, toDateCached);

        // put another days to cache
        myCache.putAll(mockTransactionsForCache);

        // check for cached with another days
        List<Transaction> cached = myCache.getAllBetween(fromDateCached, toDateCached);
        transactionsEquals(cached, mockTransactionsForCache);

        // Response
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(request.getDateFrom(), request.getDateTo());

        // check for correctly cached
        List<Transaction> fromCache = myCache
                .getAllBetween(request.getDateFrom(), request.getDateTo());
        mockTransactions.sort(Comparator.comparing(Transaction::getAddedDate));
        transactionsEquals(fromCache, mockTransactions);

        Assertions.assertNotNull(responseEntity);
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        List<TransactionDTO> body = responseEntity.getBody();

        transactionsAndDTOsEquals(mockTransactions, body);

    }

    /**
     * [1-10] range in client service we want get [1-3] range it cached in our cache,
     * and we get again this request we get from the cache
     */
    @Test
    void shouldGetFromCache() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDate fromDate = LocalDate.now().minusDays(5);
        LocalDate toDate = LocalDate.now();

        GetByDateRequest request = new GetByDateRequest(fromDate, toDate);

        // Mock response when to call client
        List<Transaction> mockTransactions = Mock
                .generateTransactions(9, 1L, fromDate, toDate);

        when(clientService.getAllByDateBetween(fromDate, toDate))
                .thenReturn(mockTransactions);

        // first call
        transactionService.getByDateBetween(request);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(fromDate, toDate);

        // check for cache is empty
        Assertions.assertFalse(myCache.isEmpty());

        // check for correctly cached
        List<Transaction> fromCache = myCache.getAllBetween(request.getDateFrom(), request.getDateTo());
        mockTransactions.sort(Comparator.comparing(Transaction::getAddedDate));
        transactionsEquals(fromCache, mockTransactions);

        // second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(fromDate, toDate);

        Assertions.assertNotNull(responseEntity);
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        List<TransactionDTO> body = responseEntity.getBody();

        transactionsAndDTOsEquals(mockTransactions, body);
    }

    /**
     * [1-10] range in client service
     * [1-6] request
     * and [1-3] from cache and [4-6] from client service
     */
    @Test
    void shouldGetFromCacheAndClient() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDate fromDate = LocalDate.now().minusDays(4);
        LocalDate toDate = LocalDate.now().minusDays(2);

        GetByDateRequest cacheReq = new GetByDateRequest(fromDate, toDate);

        // Mock response when to call client
        List<Transaction> mockTransactions = Mock
                .generateTransactions(9, 1L, cacheReq.getDateFrom(), cacheReq.getDateTo());

        when(clientService.getAllByDateBetween(cacheReq.getDateFrom(), cacheReq.getDateTo()))
                .thenReturn(mockTransactions);

        // first call
        transactionService.getByDateBetween(cacheReq);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(cacheReq.getDateFrom(), cacheReq.getDateTo());

        // Check for correctly cached transactions
        List<Transaction> fromCache = myCache
                .getAllBetween(cacheReq.getDateFrom(), cacheReq.getDateTo());
        assertNotNull(fromCache);
        transactionsEquals(fromCache, mockTransactions);

        GetByDateRequest request = new GetByDateRequest(LocalDate.now().minusDays(4), LocalDate.now());

        // Mock response when to call client
        List<Transaction> mockTransactions2 = new ArrayList<>();

        doAnswer((Answer<List<Transaction>>) invocation -> {
            LocalDate date = invocation.getArgument(0);
            for (int i = 0; i < 2; i++) {
                long index = 10 + (i * 3);
                if (date.equals(cacheReq.getDateTo().plusDays(i + 1))) {
                    List<Transaction> mock = Mock
                            .generateTransactions(3, index, date);
                    mockTransactions2.addAll(mock);
                    return mock;
                }
            }
            return new ArrayList<>();
        })
                .when(clientService)
                .getByDate(ArgumentMatchers.any());

        ResponseEntity<List<TransactionDTO>> response = transactionService
                .getByDateBetween(request);

        List<Transaction> allTransactions = new ArrayList<>(mockTransactions);
        allTransactions.addAll(mockTransactions2);

        Mockito.verify(clientService, times(2))
                .getByDate(ArgumentMatchers.any());

        Assertions.assertNotNull(response);
        List<TransactionDTO> body = response.getBody();
        Assertions.assertNotNull(body);

        // Check for correctly cached transactions
        List<Transaction> fromCacheAll = myCache
                .getAllBetween(request.getDateFrom(), request.getDateTo());

        allTransactions.sort(Comparator.comparing(Transaction::getAddedDate));

        assertNotNull(fromCacheAll);
        transactionsEquals(fromCacheAll, allTransactions);
        transactionsAndDTOsEquals(allTransactions, body);
    }

    /**
     * [1-10] in client service and
     * [1-4] is cached
     * request [1-6]
     * [5-6] in client is null
     */
    @Test
    void shouldGetFromCacheAndNullFromClient() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDate fromDate = LocalDate.now().minusDays(4);
        LocalDate toDate = LocalDate.now().minusDays(2);

        GetByDateRequest cacheReq = new GetByDateRequest(fromDate, toDate);

        // Mock response when to call client
        List<Transaction> mockTransactions = Mock.generateTransactions(9, 1L, fromDate, toDate);

        when(clientService.getAllByDateBetween(cacheReq.getDateFrom(), cacheReq.getDateTo()))
                .thenReturn(mockTransactions);

        // first call
        transactionService.getByDateBetween(cacheReq);

        Mockito.verify(clientService, times(1))
                .getAllByDateBetween(fromDate, toDate);

        // Check for correctly cached transactions
        List<Transaction> fromCache = myCache
                .getAllBetween(cacheReq.getDateFrom(), cacheReq.getDateTo());
        assertNotNull(fromCache);
        transactionsEquals(fromCache, mockTransactions);

        GetByDateRequest request = new GetByDateRequest(LocalDate.now().minusDays(4), LocalDate.now());

        // Mock response when to call client

        doAnswer((Answer<List<Transaction>>) invocation -> null)
                .when(clientService)
                .getByDate(ArgumentMatchers.any());

        ResponseEntity<List<TransactionDTO>> response = transactionService
                .getByDateBetween(request);

        List<Transaction> allTransactions = new ArrayList<>(mockTransactions);

        Mockito.verify(clientService, times(2))
                .getByDate(ArgumentMatchers.any());

        Assertions.assertNotNull(response);
        List<TransactionDTO> body = response.getBody();
        Assertions.assertNotNull(body);

        // Check for correctly cached transactions
        List<Transaction> fromCacheAll = myCache
                .getAllBetween(request.getDateFrom(), request.getDateTo());

        allTransactions.sort(Comparator.comparing(Transaction::getAddedDate));

        assertNotNull(fromCacheAll);
        transactionsEquals(fromCacheAll, allTransactions);
        transactionsAndDTOsEquals(allTransactions, body);

    }


    private void transactionsEquals(List<Transaction> actual, List<Transaction> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionsEquals(actual.get(i), expected.get(i));
    }

    private void transactionsEquals(Transaction actual, Transaction expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    private void transactionsNotEquals(List<Transaction> actual, List<Transaction> expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < actual.size(); i++) {
            transactionNotEquals(actual.get(i), expected.get(i));
        }
    }

    private void transactionNotEquals(Transaction actual, Transaction expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getId(), actual.getId());
        assertNotEquals(expected.getFromCard(), actual.getFromCard());
        assertNotEquals(expected.getToCard(), actual.getToCard());
        assertNotEquals(expected.getAmount(), actual.getAmount());
    }

    private void transactionAndDTOEquals(Transaction actual, TransactionDTO expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getAddedDate(), actual.getAddedDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    private void transactionsAndDTOsEquals(List<Transaction> actual, List<TransactionDTO> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionAndDTOEquals(actual.get(i), expected.get(i));
    }

}
