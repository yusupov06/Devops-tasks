package uz.md.synccachereactive.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.md.synccachereactive.dtos.GetByDatesRequest;
import uz.md.synccachereactive.dtos.TransactionDTO;
import uz.md.synccachereactive.service.TransactionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transaction/date-between")
    public Mono<ResponseEntity<Map<String, List<TransactionDTO>>>> getByDate(@RequestBody @Valid GetByDatesRequest request) {
        return transactionService.getByDateBetween(request);
    }

}
