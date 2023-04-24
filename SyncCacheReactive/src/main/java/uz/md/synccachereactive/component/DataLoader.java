package uz.md.synccachereactive.component;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uz.md.synccachereactive.entity.Transaction;
import uz.md.synccachereactive.repository.TransactionRepository;
import uz.md.synccachereactive.utils.MockGenerator;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile(value = {"dev", "test"})
public class DataLoader implements CommandLineRunner {
    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) {
        initData();
    }

    private void initData() {


        MockGenerator.setUzCards(8);
        MockGenerator.setUzCardTransactions(MockGenerator
                .generateMockUzCardTransactions(200));
        MockGenerator.setVisaCards(8);
        MockGenerator.setVisaTransactions(MockGenerator
                .generateMockVisaCardTransactions(200));



    }
}
