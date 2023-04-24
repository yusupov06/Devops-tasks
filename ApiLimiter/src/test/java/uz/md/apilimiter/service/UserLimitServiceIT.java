package uz.md.apilimiter.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uz.md.apilimiter.IntegrationTest;
import uz.md.apilimiter.config.TestSecurityConfig;
import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.domain.enums.TariffType;
import uz.md.apilimiter.repository.ApiLimitRepository;
import uz.md.apilimiter.repository.UserLimitRepository;
import uz.md.apilimiter.resource.SimulationResource;
import uz.md.apilimiter.service.util.ServiceUtil;
import uz.md.apilimiter.utils.CommonTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@AutoConfigureMockMvc
@ActiveProfiles("testdev")
@Import(TestSecurityConfig.class)
public class UserLimitServiceIT {

    @Autowired
    private CommonTestUtils commonTestUtils;

    @Autowired
    private ServiceUtil serviceUtil;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserLimitRepository userLimitRepository;

    @Autowired
    private ApiLimitRepository apiLimitRepository;

    @Test
    @WithMockUser(username = "yusupov")
    void shouldWorkForPackageTariff() {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.PACKAGE);

        ApiLimit apiLimit = commonTestUtils.generateApiLimit(tariff, "^/simulation/api-\\d{1}");
        apiLimitRepository.save(apiLimit);
        commonTestUtils.generateUserLimit("yusupov", apiLimit);

        List<String> apiList = List.of("/simulation/api-1", "/simulation/api-2", "/simulation/api-3");

        requestTo(apiList);
    }

    @Test
    @WithMockUser(username = "yusupov")
    void shouldBeFreeForYearlyTariff() throws Exception {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.YEARLY_FREE);

        String api = SimulationResource.SIMULATION_ENDPOINT + "/api-1";

        ApiLimit apiLimit = commonTestUtils.generateApiLimit(tariff, api);

        apiLimitRepository.save(apiLimit);

        UserLimit limit = commonTestUtils.generateUserLimit("yusupov", apiLimit);

        Long limitCountBefore = limit.getLimitCount();

        mvc.perform(get("http://localhost:8081/" + api))
                .andExpect(status().isOk());

        List<UserLimit> userLimits = userLimitRepository.findAllByUsername("yusupov");
        Assertions.assertNotNull(userLimits);
        Assertions.assertEquals(1, userLimits.size());
        UserLimit userLimit = userLimits.get(0);
        Assertions.assertNotNull(userLimit);
        Assertions.assertEquals(limitCountBefore, userLimit.getLimitCount());
    }

    @Test
    @WithMockUser(username = "yusupov")
    void shouldNotWorkIfPackageFinished() throws Exception {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.PACKAGE);
        String api = SimulationResource.SIMULATION_ENDPOINT + "/api-1";
        ApiLimit apiLimit = commonTestUtils
                .generateApiLimit(tariff, api);
        apiLimit.setLimitCount(1L);

        apiLimitRepository.save(apiLimit);

        UserLimit limit = commonTestUtils
                .generateUserLimit("yusupov", apiLimit);

        Long limitCountBefore = limit.getLimitCount();

        mvc.perform(get("http://localhost:8081/" + api))
                .andExpect(status().isOk());

        List<UserLimit> userLimits = userLimitRepository.findAllByUsername("yusupov");
        Assertions.assertNotNull(userLimits);
        Assertions.assertEquals(1, userLimits.size());
        UserLimit userLimit = userLimits.get(0);
        Assertions.assertNotNull(userLimit);
        Assertions.assertFalse(userLimit.isActive());
        Assertions.assertEquals(limitCountBefore - 1, userLimit.getLimitCount());

        mvc.perform(get("http://localhost:8081/" + api))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(username = "yusupov")
    void shouldNotAllowIfTariffExpired() throws Exception {
        Tariff tariff = commonTestUtils.generateTariff(TariffType.WEEKLY);

        String api = SimulationResource.SIMULATION_ENDPOINT + "/api-1";

        ApiLimit apiLimit = commonTestUtils
                .generateApiLimit(tariff, api);

        apiLimitRepository.save(apiLimit);

        UserLimit limit = commonTestUtils
                .generateUserLimit("yusupov", apiLimit);

        limit.setActiveFrom(Instant.now().minus(8, ChronoUnit.DAYS));
        limit.setActiveTo(Instant.now().minus(1, ChronoUnit.DAYS));

        mvc.perform(get("http://localhost:8081/" + api))
                .andExpect(status().isForbidden());

    }

    @Test
    @WithMockUser(username = "yusupov")
    void shouldNotAllowTariffIsNotExpiredButPackageIsFinished() throws Exception {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.WEEKLY);

        String api = SimulationResource.SIMULATION_ENDPOINT + "/api-1";

        ApiLimit apiLimit = commonTestUtils
                .generateApiLimit(tariff, api);

        apiLimit.setLimitCount(1L);

        apiLimitRepository.save(apiLimit);

        UserLimit limit = commonTestUtils
                .generateUserLimit("yusupov", apiLimit);

        limit.setLimitCount(0L);

        mvc.perform(get("http://localhost:8081/" + api))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "yusupov")
    void shouldAllowPatternedApisWithValidTariff() throws Exception {

        Tariff tariff = commonTestUtils.generateTariff(TariffType.WEEKLY);

        String apiPatterned = serviceUtil.getAsPattern(List.of(
                SimulationResource.SIMULATION_ENDPOINT + "/services/1",
                SimulationResource.SIMULATION_ENDPOINT + "/services/2"
        ));

        ApiLimit apiLimit = commonTestUtils.generateApiLimit(tariff,
                apiPatterned
        );

        apiLimit.setApiRegex(apiPatterned);
        apiLimitRepository.save(apiLimit);

        UserLimit limit = commonTestUtils.generateUserLimit("yusupov", apiLimit);

        Long limitCountBefore = limit.getLimitCount();

        mvc.perform(get("http://localhost:8081/simulation/services/1"))
                .andExpect(status().isOk());

        mvc.perform(get("http://localhost:8081/simulation/services/2"))
                .andExpect(status().isOk());

        List<UserLimit> userLimits = userLimitRepository.findAllByUsername("yusupov");
        Assertions.assertNotNull(userLimits);
        Assertions.assertEquals(1, userLimits.size());
        UserLimit userLimit = userLimits.get(0);
        Assertions.assertNotNull(userLimit);
        Assertions.assertNotEquals(limitCountBefore, userLimit.getLimitCount());
        Assertions.assertEquals(limitCountBefore, userLimit.getLimitCount() + 2);

    }

//    @Test
//    @WithMockUser(username = "yusupov")

    private void requestTo(List<String> apis) {
        apis.forEach(this::testThisApi);
    }

    private void testThisApi(String api) {
        try {
            mvc.perform(get("http://localhost:8081/" + api))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
