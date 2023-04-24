package uz.md.apilimiter.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.domain.enums.TariffType;
import uz.md.apilimiter.repository.ApiLimitRepository;
import uz.md.apilimiter.repository.TariffRepository;
import uz.md.apilimiter.repository.UserLimitRepository;
import uz.md.apilimiter.resource.SimulationResource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class CommonTestUtils {

    private final ApiLimitRepository apiLimitRepository;
    private final UserLimitRepository userLimitRepository;
    private final TariffRepository tariffRepository;

    public List<ApiLimit> generateApiLimits(int count, Tariff tariff) {

        return apiLimitRepository.saveAll(IntStream.range(0, count)
                .mapToObj(n -> generateApiLimit(tariff,
                        SimulationResource.SIMULATION_ENDPOINT + "/api-" + (n + 1)))
                .collect(Collectors.toList()));
    }

    public ApiLimit generateApiLimit(Tariff tariff, String apiRegex) {
        return ApiLimit.builder()
                .limitCount(100L)
                .apiRegex(apiRegex)
                .tariff(tariff)
                .priority(1)
                .build();
    }

    public List<UserLimit> generateUserLimits(String username, List<ApiLimit> apiLimits) {
        return apiLimits.stream()
                .map(apiLimit -> generateUserLimit(username, apiLimit))
                .collect(Collectors.toList());
    }

    public UserLimit generateUserLimit(String username, ApiLimit apiLimit) {
        Instant[] fromAndTo = resolveActiveDays(apiLimit);
        return userLimitRepository
                .save(UserLimit.builder()
                        .username(username)
                        .apiLimit(apiLimit)
                        .limitCount(apiLimit.getLimitCount())
                        .activeFrom(fromAndTo[0])
                        .activeTo(fromAndTo[1])
                        .active(true)
                        .build());
    }

    private Instant[] resolveActiveDays(ApiLimit apiLimit) {
        String tariffType = apiLimit.getTariff().getType().name();
        if (tariffType.startsWith("YEARLY"))
            return new Instant[]{
                    Instant.now().minus(100, ChronoUnit.DAYS),
                    Instant.now().plus(265, ChronoUnit.DAYS)
            };
        else if (tariffType.startsWith("MONTHLY")) {
            return new Instant[]{
                    Instant.now().minus(15, ChronoUnit.DAYS),
                    Instant.now().plus(15, ChronoUnit.DAYS)
            };
        } else if (tariffType.startsWith("WEEKLY")) {
            return new Instant[]{
                    Instant.now().minus(3, ChronoUnit.DAYS),
                    Instant.now().plus(4, ChronoUnit.DAYS)
            };
        } else if (tariffType.startsWith("DAILY")) {
            return new Instant[]{
                    Instant.now().minus(10, ChronoUnit.HOURS),
                    Instant.now().plus(14, ChronoUnit.HOURS)
            };
        } else
            return new Instant[]{
                    Instant.now().minus(10, ChronoUnit.DAYS),
                    Instant.now().plus(10, ChronoUnit.DAYS)
            };
    }

    public Tariff generateTariff(TariffType type) {
        return tariffRepository.save(Tariff
                .builder()
                .name("Package")
                .price(BigDecimal.valueOf(10000))
                .type(type)
                .build());
    }
}
