package uz.md.synccache.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.entity.Transaction;

import java.time.LocalDate;
import java.util.*;

@Component
@Transactional
@Slf4j
public class MyCache {

    private final Map<LocalDate, List<Transaction>> cache = new HashMap<>();

    public void put(LocalDate key, Transaction value) {
        log.info("put to cache with key: " + key + " value " + value.toString());
        if (value.getAddedDate().toLocalDate().equals(key)) {
            List<Transaction> values = cache.getOrDefault(key, new ArrayList<>());
            values.add(value);
            cache.put(key, values);
        }
    }

    public void put(LocalDate key, List<Transaction> values) {
        if (key == null || values == null)
            return;
        log.info("put to cache with key: " + key + " values " + values);
        cache.put(key, values);
    }

    public List<Transaction> getByDate(LocalDate key) {
        return cache.getOrDefault(key, null);
    }

    public void putAll(List<Transaction> list) {
        log.info(" put all to cache " + list);
        for (Transaction transaction : list) {
            put(transaction.getAddedDate().toLocalDate(), transaction);
        }
    }

    public List<Transaction> getAllBetween(LocalDate dateFrom, LocalDate dateTo) {
        log.info(" get between date " + dateFrom + " and " + dateTo);

        List<Transaction> list = new ArrayList<>();

        while (dateFrom.isBefore(dateTo)) {
            List<Transaction> transactions = cache.get(dateFrom);
            if (transactions != null)
                list.addAll(transactions);
            dateFrom = dateFrom.plusDays(1);
        }
        List<Transaction> transactions = cache.get(dateTo);
        if (transactions != null)
            list.addAll(transactions);
        list.sort(Comparator.comparing(Transaction::getAddedDate));
        return list;
    }

    public Set<LocalDate> getKeys() {
        return cache.keySet();
    }

    public void invalidateAll() {
        log.info("invalidating all");
        cache.clear();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public boolean isEmpty(LocalDate dateFrom, LocalDate dateTo) {
        while (dateFrom.isBefore(dateTo.plusDays(1))) {
            if (cache.containsKey(dateFrom))
                return false;
            dateFrom = dateFrom.plusDays(1);
        }
        return true;
    }

    public boolean containsKey(LocalDate date) {
        return cache.containsKey(date);
    }
}
