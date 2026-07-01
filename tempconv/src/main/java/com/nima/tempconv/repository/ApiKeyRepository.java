package com.nima.tempconv.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.nima.tempconv.model.ApiKey;

public interface ApiKeyRepository extends MongoRepository<ApiKey, String> {

    Optional<ApiKey> findByKeyValue(String keyValue);
}
