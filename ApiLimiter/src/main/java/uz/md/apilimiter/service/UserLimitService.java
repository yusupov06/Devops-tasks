package uz.md.apilimiter.service;

import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.service.dto.UserLimitAddDTO;

import java.util.List;
import java.util.Optional;

public interface UserLimitService {

    List<UserLimit> findAllByUsername(String username);

    boolean reduceUserLimit(UserLimit userLimit);

    UserLimit save(UserLimitAddDTO userLimit);

    UserLimit save(UserLimit userLimit);

    void resolveUserLimit(UserLimit userLimit);

    void saveAll(List<UserLimit> userLimits);

    boolean activateUserLimit(Long userLimitId);

    Optional<UserLimit> findByUsernameAndPatternedApi(String username, String requestURI);
}
