package uz.md.apilimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.md.apilimiter.domain.UserLimit;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {

    @Query(nativeQuery = true, value =
            "select u.* from user_limit u join api_limit al on al.id = u.api_limit_id " +
              "where u.username = :username and :api ~ al.api_regex " +
                    " ORDER BY al.priority limit 1")
    Optional<UserLimit> findByUsernameAndApiLimit_ApiAndApiLimit_PatternedIsTrue(String username, String api);

    List<UserLimit> findAllByUsername(String username);
}
