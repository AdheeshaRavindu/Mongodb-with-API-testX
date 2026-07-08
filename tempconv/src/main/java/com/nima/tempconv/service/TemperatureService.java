package com.nima.tempconv.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.nima.tempconv.messaging.ConversionEventProducer;
import com.nima.tempconv.model.TemperatureLog;
import com.nima.tempconv.repository.TemperatureRepository;

@Service
public class TemperatureService {

    private static final double DANGEROUS_HEAT_CELSIUS = 38.0;
    private static final double FREEZING_CELSIUS = 0.0;

    private final TemperatureRepository temperatureRepository;
    private final ConversionEventProducer conversionEventProducer;

    public TemperatureService(
            TemperatureRepository temperatureRepository,
            ConversionEventProducer conversionEventProducer) {
        this.temperatureRepository = temperatureRepository;
        this.conversionEventProducer = conversionEventProducer;
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

        TemperatureLog log = new TemperatureLog(
                null,
                value,
                formatUnit(normalizedUnit),
                outputValue,
                outputUnit,
                Instant.now().toString());

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

    public List<TemperatureLog> getHistory() {
        return temperatureRepository.findAll();
    }

    public List<TemperatureLog> getHistoryByUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Unit parameter is required.");
        }

        return temperatureRepository.findByInputUnitIgnoreCase(unit.trim());
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
