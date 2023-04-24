package uz.md.synccachereactive.clientService;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.utils.AppUtils;
import uz.md.synccachereactive.utils.MockGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@Component
public class VisaCardClient {

    public Flux<Transaction> getTransactionsBetweenDates(String cardNumber, LocalDate fromDate, LocalDate toDate) {

        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(cardNumber);
        Predicate<Transaction> datePredicate = AppUtils.datePredicate(fromDate, toDate);

        return Flux.fromIterable(MockGenerator.getVisaCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate))
                .toList());
    }
}
