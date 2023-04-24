package uz.md.synccachereactive.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.TransactionDTO;

import java.util.List;
import java.util.Map;

public interface TransactionService {

    Mono<ResponseEntity<Map<String, List<TransactionDTO>>>> getByDateBetween(GetByDatesRequest request);

    Mono<Void> checkForCachedDataAndUpdate();

}
