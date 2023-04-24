package uz.md.synccache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.clientService.ClientService;
import uz.md.synccache.config.MyCache;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.mapper.TransactionMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final MyCache myCache;
    private final ClientService clientService;

    /**
     * Main method for getting transactions between two days
     *
     * @param request {@link LocalDate} from and to your request for getting transactions between
     * @return List of transactions inside ResponseEntity
     */
    @Override
    public ResponseEntity<List<TransactionDTO>> getByDateBetween(GetByDateRequest request) {

        log.info("Getting by date between " + request);

        // Request validation
        if (request == null
                || request.getDateTo() == null
                || request.getDateFrom() == null)
            throw new BadRequestException("Request cannot be null");

        // swap request dates if invalid
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            log.info("Request dates swap");
            LocalDate dateFrom = request.getDateFrom();
            request.setDateFrom(request.getDateTo());
            request.setDateTo(dateFrom);
        }

        // if fromDate and toDate are equal we have to get by date
        if (request.getDateFrom().equals(request.getDateTo())) {

            List<Transaction> fromCache = myCache
                    .getByDate(request.getDateFrom());

            if (fromCache == null || fromCache.isEmpty()) {
                List<Transaction> fromClient = clientService
                        .getByDate(request.getDateFrom());

                if (fromClient == null)
                    fromClient = new ArrayList<>();

                return new ResponseEntity<>(transactionMapper
                        .toDTO(fromClient),
                        HttpStatus.OK);
            }

            return new ResponseEntity<>(transactionMapper
                    .toDTO(fromCache),
                    HttpStatus.OK);
        }

        // If cache has no transactions between fromDate and toDate we have call to client only
        if (myCache.isEmpty(request.getDateFrom(), request.getDateTo())) {
            List<Transaction> fromClient = clientService
                    .getAllByDateBetween(request.getDateFrom(), request.getDateTo());
            if (fromClient == null)
                fromClient = new ArrayList<>();
            myCache.putAll(fromClient);
            return new ResponseEntity<>(transactionMapper
                    .toDTO(fromClient), HttpStatus.OK);
        }

        // If cache contains transactions between fromDate and toDate
        // we get from cache if contains or else from client
        List<Transaction> transactions = getFromCacheOrElseFromClient(request.getDateFrom(), request.getDateTo());

        // response
        return new ResponseEntity<>(transactionMapper
                .toDTO(transactions), HttpStatus.OK);

    }

    /**
     * Method for main logic
     * if transactions exists in cache with date from cache or else form client
     *
     * @param date   - this date exists in cache
     * @param dateTo - till this
     * @return List of transactions
     */
    private List<Transaction> getFromCacheOrElseFromClient(LocalDate date, LocalDate dateTo) {

        List<Transaction> transactions = new ArrayList<>();

        List<LocalDate> notCachedDates = new ArrayList<>();

        // Loop through dates till dateTo
        while (date.isBefore(dateTo.plusDays(1))) {

            if (myCache.containsKey(date)) {
                if (myCache.containsKey(date.plusDays(1))) {
                    List<Transaction> cached = myCache.getByDate(date);
                    if (cached != null && !cached.isEmpty())
                        transactions.addAll(cached);
                } else
                    notCachedDates.add(date);
            } else {
                notCachedDates.add(date);
            }
            date = date.plusDays(1);
        }

        Set<Transaction> fromClient = getFromClient(notCachedDates);
        fromClient.addAll(transactions);
        return new ArrayList<>(fromClient);
    }

    private Set<Transaction> getFromClient(List<LocalDate> dates) {
        Set<Transaction> transactions = new HashSet<>();
        LocalDate from = dates.get(0);
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i-1).plusDays(1))) {
                List<Transaction> fromClient = clientService
                        .getAllByDateBetween(from, dates.get(i).minusDays(1));
                from = dates.get(i);
                transactions.addAll(fromClient);
            }
        }

        return transactions;
    }

    @Override
    public void checkForCachedDataAndUpdate() {

        Set<LocalDate> keys = myCache.getKeys();
        log.info("Checking for cached data and update all in: " + keys);

        if (keys == null || keys.isEmpty()) {
            log.info("No data in cache");
            return;
        }

        for (LocalDate key : keys) {
            List<Transaction> fromClient = clientService.getByDate(key);
            if (fromClient != null && !fromClient.isEmpty()) {
                myCache.put(key, fromClient);
            }
        }

    }


    /**
     * if (request.getDateFrom().isBefore(cachedFrom) && request.getDateTo().isAfter(cachedTo)) {
     *
     *             List<Transaction> fromClient1 = clientService
     *                     .getAllByDateBetween(request.getDateFrom(), cachedFrom.minusNanos(1));
     *             if (fromClient1 != null)
     *                 transactions.addAll(fromClient1);
     *
     *             transactions.addAll(fromCache);
     *
     *             List<Transaction> fromClient2 = clientService
     *                     .getAllByDateBetween(cachedTo.plusNanos(1), request.getDateTo());
     *             if (fromClient2 != null)
     *                 transactions.addAll(fromClient2);
     *
     *
     *         } else if (request.getDateFrom().isBefore(cachedFrom)) {
     *
     *             transactions.addAll(fromCache);
     *             List<Transaction> fromClient = clientService
     *                     .getAllByDateBetween(request.getDateFrom(), cachedFrom.minusNanos(1));
     *             if (fromClient != null)
     *                 transactions.addAll(fromClient);
     *
     *         } else if (request.getDateTo().isAfter(cachedTo)) {
     *             transactions.addAll(fromCache);
     *             List<Transaction> fromClient = clientService
     *                     .getAllByDateBetween(cachedTo.plusNanos(1), request.getDateTo());
     *             if (fromClient != null)
     *                 transactions.addAll(fromClient);
     *         } else {
     *             transactions.addAll(fromCache);
     *         }
     */

}
