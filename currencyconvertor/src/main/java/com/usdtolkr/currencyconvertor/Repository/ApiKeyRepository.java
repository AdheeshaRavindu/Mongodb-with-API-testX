package com.usdtolkr.currencyconvertor.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.usdtolkr.currencyconvertor.model.ApiKey;

public interface ApiKeyRepository extends MongoRepository<ApiKey, String> {

    Optional<ApiKey> findByKeyValue(String keyValue);
}
