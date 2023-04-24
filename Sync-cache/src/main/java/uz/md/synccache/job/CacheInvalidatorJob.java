package uz.md.synccache.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.md.synccache.config.MyCache;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class CacheInvalidatorJob {

    private final MyCache cache;

    // cron = "0 0 0 */4 * ?"
    @Scheduled(fixedDelay = 10*60000)
    public void execute() {
        cache.invalidateAll();
    }
}
