package com.usdtolkr.currencyconvertor.service;

import com.usdtolkr.currencyconvertor.messaging.ConversionEventProducer;
import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import com.usdtolkr.currencyconvertor.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final ConversionEventProducer conversionEventProducer;

    // Constant exchange rate for demonstration (e.g., 1 USD = 300 LKR)
    private static final double USD_TO_LKR_RATE = 300.0;

    public CurrencyLog convertAndSave(double amount) {
        double result = amount * USD_TO_LKR_RATE;

        CurrencyLog log = new CurrencyLog();
        log.setInputAmount(amount);
        log.setInputCurrency("USD");
        log.setOutputAmount(result);
        log.setOutputCurrency("LKR");
        log.setExchangeRate(USD_TO_LKR_RATE);
        log.setTimestamp(LocalDateTime.now().toString());

        CurrencyLog saved = currencyRepository.save(log);
        conversionEventProducer.publishCurrencyConversion(saved);
        return saved;
    }

    public List<CurrencyLog> getAllLogs() {
        return currencyRepository.findAll();
    }
}
