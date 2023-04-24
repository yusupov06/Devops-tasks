package uz.md.synccachereactive.strategy;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import uz.md.synccachereactive.clientService.UzCardClient;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.utils.AppUtils;
import uz.md.synccachereactive.utils.MockGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;
import static uz.md.synccachereactive.util.TestUtil.*;


@SpringBootTest
@ActiveProfiles("test")
public class GetTransactionsStrategyTest {

    @Autowired
    private GetTransactionsComposite composite;

    @MockBean
    private UzCardClient uzCardClient;

    private List<Transaction> getTransactionsWithCardAndFromToDate(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate){
        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(fromDate, toDate);
        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(cardNumber);
        return MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();
    }

    @Test
    void shouldGetTransactionsBetweenDates() {
        List<String> uzCards = MockGenerator.getUzCards();
        Assertions.assertNotNull(uzCards);
        List<String> cards = uzCards.subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(5);
        LocalDateTime toDate = LocalDateTime.now();

        List<Transaction> mockTransactions1 = getTransactionsWithCardAndFromToDate(cards.get(0), fromDate, toDate);
        List<Transaction> mockTransactions2 = getTransactionsWithCardAndFromToDate(cards.get(1), fromDate, toDate);

        when(uzCardClient.getTransactionsBetweenDates(cards.get(0), fromDate.toLocalDate(), toDate.toLocalDate()))
                .thenReturn(Flux.fromIterable(mockTransactions1));

        when(uzCardClient.getTransactionsBetweenDates(cards.get(1), fromDate.toLocalDate(), toDate.toLocalDate()))
                .thenReturn(Flux.fromIterable(mockTransactions2));

        Map<String, List<Transaction>> response = composite.getTransactionsBetweenDays(cards, fromDate, toDate)
                .block();

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(cards.get(0), fromDate.toLocalDate(), toDate.toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(cards.get(1), fromDate.toLocalDate(), toDate.toLocalDate());

        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.containsKey(cards.get(0)));
        List<Transaction> transactions1 = response.get(cards.get(0));
        Assertions.assertNotNull(transactions1);

        Assertions.assertTrue(response.containsKey(cards.get(1)));
        List<Transaction> transactions2 = response.get(cards.get(1));
        Assertions.assertNotNull(transactions2);

        transactionsEquals(transactions1, mockTransactions1);
        transactionsEquals(transactions2, mockTransactions2);

    }


}
