package com.usdtolkr.currencyconvertor.repository;

import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends MongoRepository<CurrencyLog, String> {
}
