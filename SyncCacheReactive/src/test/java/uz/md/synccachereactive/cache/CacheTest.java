package uz.md.synccachereactive.cache;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uz.md.synccachereactive.entity.Range;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.entity.TransactionStatus;
import uz.md.synccachereactive.repository.RangeRepository;
import uz.md.synccachereactive.repository.TransactionRepository;
import uz.md.synccachereactive.utils.AppUtils;
import uz.md.synccachereactive.utils.MockGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static uz.md.synccachereactive.util.TestUtil.transactionsEquals;

@SpringBootTest
@ActiveProfiles("test")
public class CacheTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RangeRepository rangeRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheRangeService cacheRangeService;

    private List<Transaction> getTransactionsWithCardAndFromToDate(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate) {
        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(fromDate, toDate);
        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(cardNumber);
        return MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();
    }

    @BeforeEach
    void init() {
        transactionRepository.deleteAll().block();
        rangeRepository.deleteAll().block();
    }



    @Test
    void shouldReturnTrueInIsEmptyMethod() {
        Boolean isEmpty = cacheRangeService.isEmpty()
                .block();
        Assertions.assertNotNull(isEmpty);
        Assertions.assertTrue(isEmpty);
    }

    @Test
    void shouldUpdate() {

        Transaction transaction = Transaction.builder()
                .fromCard("860012")
                .toCard("860013")
                .addedDate(LocalDateTime.now())
                .status(TransactionStatus.FAILED)
                .amount(BigDecimal.ONE)
                .build();

        cacheService.put(transaction)
                .block();

        cacheService.put(Transaction.builder()
                        .id(transaction.getId())
                        .amount(BigDecimal.TEN)
                        .status(TransactionStatus.SUCCESS)
                        .addedDate(LocalDateTime.now().plusDays(1))
                        .fromCard("8601")
                        .toCard("8600")
                        .build())
                .block();

    }

    @Test
    void shouldReturnFalseInIsEmptyMethod() {
        Range range = Range.builder()
                .fromDate(LocalDateTime.now().minusDays(5))
                .cardNumber("8600")
                .toDate(LocalDateTime.now())
                .build();
        rangeRepository.save(range).block();
        Boolean isEmpty = cacheRangeService.isEmpty().block();
        Assertions.assertNotNull(isEmpty);
        Assertions.assertFalse(isEmpty);
    }

    @Test
    void shouldTrueInIsEmptyMethodWithDates() {
        Range range = Range.builder()
                .fromDate(LocalDateTime.now().minusDays(5))
                .cardNumber("8600")
                .toDate(LocalDateTime.now())
                .build();
        rangeRepository.save(range).block();
        Boolean isEmpty = cacheRangeService.isEmpty(range.getCardNumber(), LocalDateTime.now().minusDays(6), LocalDateTime.now().plusDays(1))
                .block();
        Assertions.assertNotNull(isEmpty);
        Assertions.assertFalse(isEmpty);
    }

    @Test
    void shouldExistsByCardNumber() {
        Range range = Range.builder()
                .fromDate(LocalDateTime.now().minusDays(5))
                .cardNumber("8600")
                .toDate(LocalDateTime.now())
                .build();
        rangeRepository.save(range).block();
        Boolean existed = cacheRangeService.existsCacheRangeByCardNumber(range.getCardNumber())
                .block();
        Assertions.assertNotNull(existed);
        Assertions.assertTrue(existed);
    }

    @Test
    void shouldGetAllBetween() {
        List<String> uzCards = MockGenerator.getUzCards();
        Assertions.assertNotNull(uzCards);
        String card = uzCards.get(0);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(5);
        LocalDateTime toDate = LocalDateTime.now();
        List<Transaction> transactions = getTransactionsWithCardAndFromToDate(card, fromDate, toDate);

        List<Transaction> list = transactionRepository
                .saveAll(transactions)
                .collectList()
                .block();

        Assertions.assertNotNull(list);

        list.sort(Comparator.comparing(Transaction::getAddedDate));

        List<Transaction> fromCache = cacheService.getAllBetween(card, fromDate, toDate)
                .collectList()
                .block();
        Assertions.assertNotNull(fromCache);
        transactionsEquals(fromCache, transactions);
    }

    @Test
    void shouldPutTransaction() {
        List<Transaction> uzCardTransactions = MockGenerator.getUzCardTransactions();
        Assertions.assertNotNull(uzCardTransactions);
        Transaction transaction = uzCardTransactions.get(0);
        cacheService.put(transaction).block();
        List<Transaction> fromDB = transactionRepository
                .findAll()
                .collectList()
                .block();
        Assertions.assertNotNull(fromDB);
        Assertions.assertEquals(1, fromDB.size());
        transactionsEquals(fromDB.get(0), transaction);
    }

    @Test
    void shouldPutAll() {
        List<Transaction> uzCardTransactions = MockGenerator.getUzCardTransactions();
        Assertions.assertNotNull(uzCardTransactions);
        Assertions.assertTrue(uzCardTransactions.size() > 10);
        List<Transaction> transactions = new ArrayList<>(uzCardTransactions.subList(0, 10));
        transactions.sort(Comparator.comparing(Transaction::getAddedDate));
        cacheService.putAll(transactions).block();
        List<Transaction> fromDB = transactionRepository.findAllSorted()
                .collectList()
                .block();

        Assertions.assertNotNull(fromDB);
        Assertions.assertEquals(fromDB.size(), 10);
        transactionsEquals(transactions, fromDB);
    }


}
