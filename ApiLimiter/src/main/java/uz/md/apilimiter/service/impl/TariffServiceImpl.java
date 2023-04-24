package uz.md.apilimiter.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.apilimiter.domain.Tariff;
import uz.md.apilimiter.repository.TariffRepository;
import uz.md.apilimiter.service.TariffService;
import uz.md.apilimiter.service.dto.TariffAddDTO;

import java.util.List;
import java.util.Optional;

/**
 * @author Muhammadqodir
 * CRUD service for {@link Tariff}
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;

    @Override
    public Tariff save(TariffAddDTO addDTO) {
        Tariff tariff = mapToTariff(addDTO);
        return tariffRepository.save(tariff);
    }

    private Tariff mapToTariff(TariffAddDTO addDTO) {
        return new Tariff(addDTO.getName(),
                addDTO.getType(),
                addDTO.getPrice());
    }

    @Override
    public List<Tariff> findAll() {
        return tariffRepository
                .findAll();
    }

    @Override
    public Optional<Tariff> findOne(Long id) {
        return tariffRepository
                .findById(id);
    }

    @Override
    public void delete(Long id) {
        tariffRepository.deleteById(id);
    }
}
