package com.nima.tempconv.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.nima.tempconv.model.TemperatureLog;

public interface TemperatureRepository extends MongoRepository<TemperatureLog, String> {

    List<TemperatureLog> findByInputUnit(String inputUnit);
}
