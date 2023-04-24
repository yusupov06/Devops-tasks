package uz.md.apilimiter.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.domain.enums.TariffType;
import uz.md.apilimiter.errors.CodedException;
import uz.md.apilimiter.repository.ApiLimitRepository;
import uz.md.apilimiter.repository.UserLimitRepository;
import uz.md.apilimiter.service.UserLimitService;
import uz.md.apilimiter.service.dto.UserLimitAddDTO;
import uz.md.apilimiter.service.util.ServiceUtil;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * service for CRUD {@link UserLimit} and this domain related methods
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserLimitServiceImpl implements UserLimitService {

    private final UserLimitRepository userLimitRepository;
    private final ServiceUtil serviceUtil;
    private final ApiLimitRepository apiLimitRepository;

    @Override
    public List<UserLimit> findAllByUsername(String username) {
        log.info("findAllByUsername is working");
        return userLimitRepository.findAll().stream()
                .filter(userLimit -> userLimit.getUsername().equals(username))
                .collect(Collectors.toList());
    }

    @Override
    public boolean reduceUserLimit(@NotNull UserLimit userLimit) {
        userLimit.setLimitCount(userLimit.getLimitCount() - 1);
        if (userLimit.getLimitCount() == 0)
            userLimit.setActive(false);
        userLimitRepository.save(userLimit);
        return false;
    }

    @Override
    public UserLimit save(UserLimitAddDTO addDTO) {
        if (addDTO == null
                || addDTO.getUsername() == null
                || addDTO.getApiLimitId() == null)
            throw new CodedException("Bad request", HttpStatus.BAD_REQUEST.value());

        ApiLimit apiLimit = apiLimitRepository.findById(addDTO.getApiLimitId())
                .orElseThrow(() -> new CodedException("ApiLimit not found with id: " + addDTO.getApiLimitId(),
                        HttpStatus.NOT_FOUND.value()));

        UserLimit userLimit = new UserLimit();

        userLimit.setApiLimit(apiLimit);
        userLimit.setLimitCount(apiLimit.getLimitCount());
        userLimit.setUsername(addDTO.getUsername());

        Instant[] days = serviceUtil.getActiveDaysForType(apiLimit.getTariff().getType());
        userLimit.setActiveFrom(days[0]);
        userLimit.setActiveTo(days[1]);

        return userLimitRepository.save(userLimit);
    }

    @Override
    public UserLimit save(UserLimit userLimit) {
        return userLimitRepository.save(userLimit);
    }

    @Override
    public void resolveUserLimit(@NotNull UserLimit userLimit) {

        // check for user limit is active
        if (!userLimit.isActive()) {
            log.info("User with username " + userLimit.getUsername() + " is not active for this api: " + userLimit.getApiLimit().getApiRegex());
            throw new CodedException("You are not active for this api",
                    HttpStatus.FORBIDDEN.value());
        }

        // check for user limit and package
        // ex. '/api' api is PACKAGE tariff and limitCount 100 it is not related from and to dates
        if (serviceUtil.isPackageTariff(userLimit.getApiLimit().getTariff())) {
            log.info("User with username {} has package tariff: {} ",
                    userLimit.getUsername(),
                    userLimit.getApiLimit());
            if (reduceUserLimit(userLimit)) {
                log.info("Something went wrong while reduce user limit");
                throw new CodedException("Something went wrong while reduce user limit",
                        HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            log.info("User with username {} limit successfully reduced ", userLimit.getUsername());
            return;
        }

        // check for user limit is active for range
        if (!serviceUtil.isActiveNow(userLimit)) {
            log.info("User with username " + userLimit.getUsername() + " is not active for this api: " + userLimit.getApiLimit().getApiRegex());
            throw new CodedException("You are not active for this api",
                    HttpStatus.FORBIDDEN.value());
        }

        // check for user limit is free and range is active
        // ex. '/api' api is free for some range and, it is paid
        if (serviceUtil.isFreePrefixed(userLimit.getApiLimit().getTariff())) {
            log.info("User with username {} has active and free tariff: {} ",
                    userLimit.getUsername(),
                    userLimit.getApiLimit());
            return;
        }

        // check for user limit for YEARLY, WEEKLY like tariffs
        if (userLimit.getLimitCount() > 0) {
            if (reduceUserLimit(userLimit)) {
                log.info("Something went wrong while reduce user limit");
                throw new CodedException("Something went wrong while reduce user limit",
                        HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            log.info("User is active but no limits left");
            throw new CodedException("You have no limits left",
                    HttpStatus.FORBIDDEN.value());
        }

        log.info("User with username {} limit successfully reduced ", userLimit.getUsername());
    }

    @Override
    public void saveAll(List<UserLimit> userLimits) {
        userLimitRepository.saveAll(userLimits);
    }

    @Override
    public boolean activateUserLimit(Long userLimitId) {
        UserLimit userLimit = userLimitRepository.findById(userLimitId)
                .orElseThrow(() -> new CodedException("UserLimit not found with id: " + userLimitId,
                        HttpStatus.NOT_FOUND.value()));
        return activate(userLimit);
    }

    @Override
    public Optional<UserLimit> findByUsernameAndPatternedApi(String username, String requestURI) {
        if (!serviceUtil.isValidApi(requestURI))
            throw new CodedException("Requested API is invalid", HttpStatus.BAD_REQUEST.value());

        return userLimitRepository
                .findByUsernameAndApiLimit_ApiAndApiLimit_PatternedIsTrue(
                        username, requestURI
                );
    }

    private boolean activate(@NotNull UserLimit userLimit) {

        ApiLimit apiLimit = userLimit.getApiLimit();
        Tariff tariff = apiLimit.getTariff();
        TariffType type = tariff.getType();

        if (!type.equals(TariffType.PACKAGE)){
            Instant[] days = serviceUtil.getActiveDaysForType(type);
            userLimit.setActiveFrom(days[0]);
            userLimit.setActiveTo(days[1]);
        }

        userLimit.setActive(true);
        userLimit.setLimitCount(apiLimit.getLimitCount());
        return true;
    }

}
