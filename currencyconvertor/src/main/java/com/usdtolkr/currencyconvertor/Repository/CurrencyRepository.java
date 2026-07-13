package com.usdtolkr.currencyconvertor.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.usdtolkr.currencyconvertor.model.CurrencyLog;

@Repository
public interface CurrencyRepository extends MongoRepository<CurrencyLog, String> {

    Page<CurrencyLog> findByUserId(String userId, Pageable pageable);

    List<CurrencyLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    long countByUserId(String userId);

    List<CurrencyLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
