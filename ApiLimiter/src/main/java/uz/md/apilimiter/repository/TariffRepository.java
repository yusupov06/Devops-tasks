package uz.md.apilimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.md.apilimiter.domain.Tariff;

@Repository
public interface TariffRepository extends JpaRepository<Tariff,Long> {
}
