package uz.md.apilimiter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.errors.CodedException;
import uz.md.apilimiter.repository.ApiLimitRepository;
import uz.md.apilimiter.repository.TariffRepository;
import uz.md.apilimiter.service.ApiLimitService;
import uz.md.apilimiter.service.dto.ApiLimitAddDTO;
import uz.md.apilimiter.service.util.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD service for {@link ApiLimit}
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApiLimitServiceImpl implements ApiLimitService {

    private final ApiLimitRepository apiLimitRepository;
    private final TariffRepository tariffRepository;
    private final ServiceUtil serviceUtil;

    @Override
    public ApiLimit add(ApiLimitAddDTO addDTO) {
        ApiLimit apiLimit = mapToApiLimit(addDTO);
        return apiLimitRepository
                .save(apiLimit);
    }

    private ApiLimit mapToApiLimit(ApiLimitAddDTO addDTO) {
        Tariff tariff = tariffRepository.findById(addDTO.getTariffId())
                .orElseThrow(() -> new CodedException("Tariff not found", HttpStatus.NO_CONTENT.value()));
        return new ApiLimit(addDTO.getApiRegex(), addDTO.getLimitCount(), addDTO.getPriority(), tariff);
    }

    @Override
    public List<ApiLimit> findAll() {
        return new ArrayList<>(apiLimitRepository
                .findAll());
    }

    @Override
    public ApiLimit findById(Long id) {
        return apiLimitRepository
                .findById(id)
                .orElseThrow(() -> new CodedException("ApiLimit not found", 404));
    }
}
