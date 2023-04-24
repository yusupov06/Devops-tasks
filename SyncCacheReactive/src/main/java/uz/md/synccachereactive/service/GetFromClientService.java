package uz.md.synccachereactive.service;

import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.TransactionDTO;

import java.util.List;
import java.util.Map;

public interface GetFromClientService {

    Mono<Map<String, List<TransactionDTO>>> getAllFromClient(List<String> notCached, GetByDatesRequest request);
}
