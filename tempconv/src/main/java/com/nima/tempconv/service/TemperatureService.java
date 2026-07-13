package com.nima.tempconv.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.nima.tempconv.dto.PagedResponse;
import com.nima.tempconv.dto.TemperatureStatsResponse;
import com.nima.tempconv.exception.ResourceNotFoundException;
import com.nima.tempconv.messaging.ConversionEventProducer;
import com.nima.tempconv.model.TemperatureLog;
import com.nima.tempconv.repository.TemperatureRepository;
import com.nima.tempconv.security.CurrentUserService;

@Service
public class TemperatureService {

    private static final double DANGEROUS_HEAT_CELSIUS = 38.0;
    private static final double FREEZING_CELSIUS = 0.0;
    private static final List<String> SUPPORTED_UNITS = List.of("Celsius", "Fahrenheit", "Kelvin");

    private final TemperatureRepository temperatureRepository;
    private final ConversionEventProducer conversionEventProducer;
    private final CurrentUserService currentUserService;

    public TemperatureService(
            TemperatureRepository temperatureRepository,
            ConversionEventProducer conversionEventProducer,
            CurrentUserService currentUserService) {
        this.temperatureRepository = temperatureRepository;
        this.conversionEventProducer = conversionEventProducer;
        this.currentUserService = currentUserService;
    }

    public TemperatureLog convertAndSave(double value, String unit) {
        String normalizedUnit = normalize(unit);
        double outputValue;
        String outputUnit;

        switch (normalizedUnit) {
            case "celsius":
                outputValue = (value * 1.8) + 32;
                outputUnit = "Fahrenheit";
                break;
            case "fahrenheit":
                outputValue = (value - 32) / 1.8;
                outputUnit = "Celsius";
                break;
            case "kelvin":
                outputValue = value - 273.15;
                outputUnit = "Celsius";
                break;
            default:
                throw new IllegalArgumentException("Unsupported unit. Use Celsius, Fahrenheit, or Kelvin.");
        }

        TemperatureLog log = new TemperatureLog();
        log.setInputTemperature(value);
        log.setInputUnit(formatUnit(normalizedUnit));
        log.setOutputTemperature(outputValue);
        log.setOutputUnit(outputUnit);
        log.setTimestamp(Instant.now().toString());
        log.setUserId(currentUserService.requireUserId());

        TemperatureLog saved = temperatureRepository.save(log);
        conversionEventProducer.publishTemperatureConversion(saved);
        return saved;
    }

    public String getSafetyWarning(double value, String unit) {
        String normalizedUnit = normalize(unit);
        if (normalizedUnit.isEmpty() || !isSupportedUnit(normalizedUnit)) {
            throw new IllegalArgumentException("Unsupported unit. Use Celsius, Fahrenheit, or Kelvin.");
        }

        double celsius = toCelsius(value, normalizedUnit);

        if (celsius >= DANGEROUS_HEAT_CELSIUS) {
            return String.format(
                    Locale.US,
                    "Warning: %.1f%s is dangerously HOT! Stay hydrated.",
                    value,
                    unitSymbol(normalizedUnit));
        }

        if (celsius <= FREEZING_CELSIUS) {
            return String.format(
                    Locale.US,
                    "Warning: %.1f%s is at freezing level or below. Take care in icy conditions.",
                    value,
                    unitSymbol(normalizedUnit));
        }

        return "The temperature is comfortable and safe.";
    }

    public List<String> getSupportedUnits() {
        return SUPPORTED_UNITS;
    }

    public List<TemperatureLog> getHistory() {
        return temperatureRepository.findAll();
    }

    public PagedResponse<TemperatureLog> getHistoryPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TemperatureLog> result = temperatureRepository.findAll(pageable);
        return toPagedResponse(result);
    }

    public List<TemperatureLog> getHistoryByUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Unit parameter is required.");
        }

        return temperatureRepository.findByInputUnitIgnoreCase(unit.trim());
    }

    public List<TemperatureLog> getLatestHistory(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return temperatureRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public List<TemperatureLog> getMyHistory() {
        String userId = currentUserService.requireUserId();
        return temperatureRepository.findByUserIdOrderByTimestampDesc(
                userId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    public PagedResponse<TemperatureLog> getMyHistoryPaged(int page, int size) {
        String userId = currentUserService.requireUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TemperatureLog> result = temperatureRepository.findByUserId(userId, pageable);
        return toPagedResponse(result);
    }

    public long getHistoryCount() {
        return temperatureRepository.count();
    }

    public long getMyHistoryCount() {
        return temperatureRepository.countByUserId(currentUserService.requireUserId());
    }

    public TemperatureLog getHistoryById(String id) {
        return temperatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Temperature conversion not found: " + id));
    }

    public void deleteHistoryById(String id) {
        if (!temperatureRepository.existsById(id)) {
            throw new ResourceNotFoundException("Temperature conversion not found: " + id);
        }
        temperatureRepository.deleteById(id);
    }

    public TemperatureStatsResponse getStats() {
        List<TemperatureLog> logs = temperatureRepository.findAll();
        Map<String, Long> byUnit = new HashMap<>();
        for (String unit : SUPPORTED_UNITS) {
            byUnit.put(unit, 0L);
        }
        for (TemperatureLog log : logs) {
            byUnit.merge(log.getInputUnit(), 1L, Long::sum);
        }
        return new TemperatureStatsResponse(logs.size(), byUnit);
    }

    private PagedResponse<TemperatureLog> toPagedResponse(Page<TemperatureLog> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private double toCelsius(double value, String normalizedUnit) {
        return switch (normalizedUnit) {
            case "celsius" -> value;
            case "fahrenheit" -> (value - 32) / 1.8;
            case "kelvin" -> value - 273.15;
            default -> throw new IllegalArgumentException("Unsupported unit. Use Celsius, Fahrenheit, or Kelvin.");
        };
    }

    private String unitSymbol(String normalizedUnit) {
        return switch (normalizedUnit) {
            case "celsius" -> "°C";
            case "fahrenheit" -> "°F";
            case "kelvin" -> "K";
            default -> "";
        };
    }

    private boolean isSupportedUnit(String normalizedUnit) {
        return switch (normalizedUnit) {
            case "celsius", "fahrenheit", "kelvin" -> true;
            default -> false;
        };
    }

    private String normalize(String unit) {
        if (unit == null) {
            return "";
        }

        String value = unit.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "c", "celcius", "celsius" -> "celsius";
            case "f", "fahrenheit" -> "fahrenheit";
            case "k", "kelvin" -> "kelvin";
            default -> value;
        };
    }

    private String formatUnit(String normalizedUnit) {
        return switch (normalizedUnit) {
            case "celsius" -> "Celsius";
            case "fahrenheit" -> "Fahrenheit";
            case "kelvin" -> "Kelvin";
            default -> normalizedUnit;
        };
    }
}
