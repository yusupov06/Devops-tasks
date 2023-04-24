package uz.md.apilimiter.service;

import uz.md.apilimiter.domain.ApiLimit;
import uz.md.apilimiter.service.dto.ApiLimitAddDTO;

import java.util.List;

public interface ApiLimitService {

    ApiLimit add(ApiLimitAddDTO addDTO);

    List<ApiLimit> findAll();

    ApiLimit findById(Long id);

}
