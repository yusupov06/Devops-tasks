package uz.md.apilimiter.service;

import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.service.dto.TariffAddDTO;

import java.util.List;
import java.util.Optional;

public interface TariffService {

    Tariff save(TariffAddDTO tariff);

    List<Tariff> findAll();

    Optional<Tariff> findOne(Long id);

    void delete(Long id);
}
