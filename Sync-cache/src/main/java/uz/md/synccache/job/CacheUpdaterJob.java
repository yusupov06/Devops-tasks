package uz.md.synccache.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.md.synccache.service.TransactionService;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class CacheUpdaterJob {

    private final TransactionService transactionService;

    // cron = "0 0 0 */2 * ?"
    @Scheduled(fixedDelay = 8*60000)
    public void execute() {
        transactionService.checkForCachedDataAndUpdate();
    }
}
