package com.nima.tempconv.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.nima.tempconv.model.TemperatureLog;

public interface TemperatureRepository extends MongoRepository<TemperatureLog, String> {

    List<TemperatureLog> findByInputUnit(String inputUnit);

    List<TemperatureLog> findByInputUnitIgnoreCase(String inputUnit);

    Page<TemperatureLog> findByUserId(String userId, Pageable pageable);

    List<TemperatureLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    long countByUserId(String userId);

    List<TemperatureLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
