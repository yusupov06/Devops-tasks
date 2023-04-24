package uz.md.apilimiter.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.apilimiter.domain.UserLimit;
import uz.md.apilimiter.errors.CodedException;
import uz.md.apilimiter.service.UserLimitService;
import uz.md.apilimiter.utils.CommonUtils;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApiLimiterAspect {

    private final UserLimitService userLimitService;

    @Before("@annotation(apiLimited)")
    public void rateLimit(ApiLimited apiLimited) throws Throwable {

        log.info("Api limiter filter working");

        HttpServletRequest request = CommonUtils.currentRequest();

        String requestURI = request.getRequestURI();

        Optional<String> currentUsername = CommonUtils.getCurrentUserLogin();

        if (currentUsername.isEmpty())
            throw new CodedException("User not found", HttpStatus.NOT_FOUND.value());

        log.info("Filter is working with current user: {}", currentUsername);

        setFilter(currentUsername.get(), requestURI);

    }


    private void setFilter(String username, String requestURI) {

        log.info("Setting filter for user: " + username + " with api: " + requestURI);

        synchronized (username.intern()) {

            log.info("Setting filter Inside method for user: " + username);

            Optional<UserLimit> limit;

                limit = userLimitService
                        .findByUsernameAndPatternedApi(username, requestURI);

            if (limit.isEmpty() || limit.get().getApiLimit() == null) {
                log.info("User with username: '" + username + "' has no permission for this api: '" + requestURI + "'");
                throw new CodedException("You have no permission for this api",
                        HttpStatus.FORBIDDEN.value());
            }

            UserLimit userLimit = limit.get();
            userLimitService.resolveUserLimit(userLimit);
        }

    }

}