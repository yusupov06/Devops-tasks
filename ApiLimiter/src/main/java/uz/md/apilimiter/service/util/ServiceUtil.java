package uz.md.apilimiter.service.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.domain.enums.TariffType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Transactional
public class ServiceUtil {

    public boolean checkForActiveAndFree(UserLimit userLimit) {
        return isActiveNow(userLimit)
                && isFreePrefixed(userLimit.getApiLimit().getTariff());
    }

    public boolean isActiveNow(UserLimit userLimit) {
        return userLimit.getActiveFrom().isBefore(Instant.now())
                && userLimit.getActiveTo().isAfter(Instant.now());
    }

    public boolean isFreePrefixed(Tariff tariff) {
        return tariff.getType() != null && tariff.getType().name().endsWith("FREE");
    }

    public boolean isPackageTariff(Tariff tariff) {
        return tariff.getType()
                .equals(TariffType.PACKAGE);
    }

    public Instant[] getActiveDaysForType(TariffType type) {

        String tariffType = type.name();

        if (tariffType.startsWith("YEARLY"))
            return new Instant[]{
                    Instant.now(),
                    Instant.now().plus(365, ChronoUnit.DAYS)
            };
        else if (tariffType.startsWith("MONTHLY")) {
            return new Instant[]{
                    Instant.now(),
                    Instant.now().plus(30, ChronoUnit.DAYS)
            };
        } else if (tariffType.startsWith("WEEKLY")) {
            return new Instant[]{
                    Instant.now(),
                    Instant.now().plus(7, ChronoUnit.DAYS)
            };
        } else if (tariffType.startsWith("DAILY")) {
            return new Instant[]{
                    Instant.now(),
                    Instant.now().plus(1, ChronoUnit.DAYS)
            };
        } else
            return new Instant[]{
                    Instant.now().minus(10, ChronoUnit.DAYS),
                    Instant.now().plus(10, ChronoUnit.DAYS)
            };
    }

    public String getAsPattern(List<String> apiList) {
        return String.join("|", apiList);
    }

    public boolean isValidApi(String requestURI) {
        return requestURI.startsWith("/") && !requestURI.contains("%");
    }

}
