package uz.md.synccache.component;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uz.md.synccache.clientService.TransactionRepository;
import uz.md.synccache.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile(value = "dev")
public class DataLoader implements CommandLineRunner {

    private final TransactionRepository transactionRepository;

    @Override
    public void run(String... args) {
        initData();
    }

    private void initData() {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            double p = Math.round(random.nextDouble() * 500) + 100.0;
            String repeat = String.valueOf(i % 10).repeat(12);
            int n = random.nextInt(10);
            transactionRepository.save(Transaction.builder()
                    .amount(BigDecimal.valueOf(p))
                    .fromCard("8600" + repeat)
                    .toCard("9860" + repeat)
                    .addedDate(LocalDateTime.now().minusDays(n))
                    .status("SUCCESS")
                    .build());
        }
    }
}
