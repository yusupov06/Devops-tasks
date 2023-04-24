package uz.md.apilimiter.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import uz.md.apilimiter.IntegrationTest;
import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.domain.enums.TariffType;
import uz.md.apilimiter.repository.ApiLimitRepository;
import uz.md.apilimiter.repository.UserLimitRepository;
import uz.md.apilimiter.service.dto.UserLimitAddDTO;
import uz.md.apilimiter.service.util.ServiceUtil;
import uz.md.apilimiter.utils.CommonTestUtils;

import java.util.List;
import java.util.Optional;

@IntegrationTest
@AutoConfigureMockMvc
@ActiveProfiles("testdev")
public class UserLimitServiceTest {

    @Autowired
    private CommonTestUtils commonTestUtils;

    @Autowired
    private UserLimitRepository userLimitRepository;

    @Autowired
    private UserLimitService userLimitService;

    @Autowired
    private ApiLimitRepository apiLimitRepository;

    @Test
    void shouldFindAllLimitsByUsername() {
        Tariff tariff = commonTestUtils.generateTariff(TariffType.DAILY);

        ApiLimit apiLimit1 = apiLimitRepository.save(commonTestUtils
                .generateApiLimit(tariff, "api/v1/users/get-all"));

        ApiLimit apiLimit2 = apiLimitRepository.save(commonTestUtils
                .generateApiLimit(tariff, "api/v1/clients/get-all"));
        String username = "yusupov";
        commonTestUtils.generateUserLimit(username, apiLimit1);
        commonTestUtils.generateUserLimit(username, apiLimit2);

        List<UserLimit> all = userLimitService
                .findAllByUsername(username);

        Assertions.assertNotNull(all);
        Assertions.assertEquals(all.size(), 2);

    }

    @Test
    void shouldReduceUserLimit() {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.DAILY);

        ApiLimit apiLimit1 = apiLimitRepository.save(commonTestUtils
                .generateApiLimit(tariff, "api/v1/users/get-all"));
        String username = "yusupov";

        UserLimit userLimit = commonTestUtils.generateUserLimit(username, apiLimit1);

        Long limitCountBefore = userLimit.getLimitCount();

        userLimitService.reduceUserLimit(userLimit);

        List<UserLimit> limits = userLimitRepository.findAllByUsername(username);
        Assertions.assertNotNull(limits);
        Assertions.assertEquals(limits.size(), 1);
        UserLimit userLimit1 = limits.get(0);
        Assertions.assertEquals(userLimit1.getLimitCount(), limitCountBefore - 1);
    }

    @Test
    void shouldAddUserLimit() {
        Tariff tariff = commonTestUtils.generateTariff(TariffType.DAILY);

        ApiLimit apiLimit = apiLimitRepository.save(commonTestUtils
                .generateApiLimit(tariff, "^/api/v1/users/get-all/*"));
        String username = "yusupov";
        UserLimitAddDTO addDTO = new UserLimitAddDTO();
        addDTO.setApiLimitId(apiLimit.getId());
        addDTO.setUsername(username);

        UserLimit userLimit = userLimitService.save(addDTO);
        Assertions.assertNotNull(userLimit);
        List<UserLimit> all = userLimitRepository.findAll();
        Assertions.assertEquals(all.size(), 1);
        UserLimit fromDB = all.get(0);
        Assertions.assertNotNull(fromDB);
        Assertions.assertEquals(fromDB.getLimitCount(), userLimit.getLimitCount());
        Assertions.assertEquals(fromDB.getUsername(), userLimit.getUsername());
        Assertions.assertEquals(fromDB.getApiLimit().getApiRegex(), userLimit.getApiLimit().getApiRegex());

        Assertions.assertEquals(userLimit.getActiveFrom(), fromDB.getActiveFrom());
        Assertions.assertEquals(userLimit.getActiveTo(), fromDB.getActiveTo());

    }

    @Test
    void shouldFindLimitByUsernameAndApi() {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.DAILY);

        ApiLimit apiLimit = apiLimitRepository.save(commonTestUtils
                .generateApiLimit(tariff, "/api/v1/users/get-all1|/api/v1/users/get-all2"));

        commonTestUtils.generateUserLimit("yusupov", apiLimit);

        Optional<UserLimit> userLimit = userLimitService
                .findByUsernameAndPatternedApi("yusupov", "/api/v1/users/get-all1");

        Assertions.assertNotNull(userLimit);
        Assertions.assertTrue(userLimit.isPresent());
        UserLimit limit = userLimit.get();
        Assertions.assertEquals(limit.getLimitCount(), apiLimit.getLimitCount());
        Assertions.assertEquals(limit.getApiLimit().getApiRegex(), apiLimit.getApiRegex());
    }

    @Test
    void shouldFindLimitByUsernameAndApiWithPriority() {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.DAILY);
        ApiLimit apiLimit = commonTestUtils
                .generateApiLimit(tariff, "^/api/\\d{2}");
        apiLimit.setPriority(1);
        apiLimitRepository.save(apiLimit);

        commonTestUtils.generateUserLimit("yusupov", apiLimit);

        ApiLimit apiLimit2 = commonTestUtils
                .generateApiLimit(tariff, "^/api/\\d{3}");
        apiLimit2.setPriority(2);
        apiLimitRepository.save(apiLimit2);

        commonTestUtils.generateUserLimit("yusupov", apiLimit2);

        Optional<UserLimit> userLimit = userLimitService
                .findByUsernameAndPatternedApi("yusupov", "/api/123");

        Assertions.assertNotNull(userLimit);
        Assertions.assertTrue(userLimit.isPresent());
        UserLimit limit = userLimit.get();
        Assertions.assertEquals(limit.getLimitCount(), apiLimit.getLimitCount());
        Assertions.assertEquals(limit.getApiLimit().getApiRegex(), apiLimit.getApiRegex());
    }

}
