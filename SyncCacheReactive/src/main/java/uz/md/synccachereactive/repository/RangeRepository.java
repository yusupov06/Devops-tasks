package uz.md.synccachereactive.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.entity.Range;

import java.io.Flushable;
import java.util.List;

@Repository
public interface RangeRepository extends R2dbcRepository<Range, Long> {

    Mono<Range> findByCardNumber(String cardNumber);

    Mono<Boolean> existsByCardNumber(String cardNumber);

    Mono<Void> deleteByCardNumber(String cardNumber);


}
