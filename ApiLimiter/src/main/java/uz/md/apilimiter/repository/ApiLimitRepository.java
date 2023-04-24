package uz.md.apilimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.md.apilimiter.domain.ApiLimit;

import java.util.Optional;

@Repository
public interface ApiLimitRepository extends JpaRepository<ApiLimit,Long> {
}