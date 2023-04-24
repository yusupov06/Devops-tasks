package uz.md.apilimiter.reset;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.repository.UserLimitRepository;

import java.time.Instant;

@EnableScheduling
@RequiredArgsConstructor
public class JobScheduler {

    private final UserLimitRepository userLimitRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void deactivateUserLimitsIfTariffFinished() {
        userLimitRepository.findAll()
                .stream()
                .filter(this::isFinished)
                .forEach(this::deactivate);
    }

    private void deactivate(UserLimit userLimit) {
        userLimit.setActive(false);
        userLimitRepository.save(userLimit);
    }

    private boolean isFinished(UserLimit userLimit) {
        Instant now = Instant.now();
        return userLimit.getActiveFrom().isAfter(now)
                || userLimit.getActiveTo().isBefore(now);
    }

}
