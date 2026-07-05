package com.usdtolkr.currencyconvertor.service;

import com.usdtolkr.currencyconvertor.repository.ApiKeyRepository;
import com.usdtolkr.currencyconvertor.exception.UnauthorizedApiKeyException;
import com.usdtolkr.currencyconvertor.model.ApiKey;
import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import com.usdtolkr.currencyconvertor.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final ApiKeyRepository apiKeyRepository;

    // Constant exchange rate for demonstration (e.g., 1 USD = 300 LKR)
    private static final double USD_TO_LKR_RATE = 300.0;

    public void validateApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new UnauthorizedApiKeyException("Missing required header X-API-KEY");
        }

        ApiKey storedKey = apiKeyRepository.findByKeyValue(apiKey.trim())
                .orElseThrow(() -> new UnauthorizedApiKeyException("Invalid or inactive API key"));

        if (!storedKey.isActive()) {
            throw new UnauthorizedApiKeyException("Invalid or inactive API key");
        }
    }

    public CurrencyLog convertAndSave(double amount) {
        double result = amount * USD_TO_LKR_RATE;

        CurrencyLog log = new CurrencyLog();
        log.setInputAmount(amount);
        log.setInputCurrency("USD");
        log.setOutputAmount(result);
        log.setOutputCurrency("LKR");
        log.setExchangeRate(USD_TO_LKR_RATE);
        log.setTimestamp(LocalDateTime.now().toString());

        return currencyRepository.save(log);
    }

    public List<CurrencyLog> getAllLogs() {
        return currencyRepository.findAll();
    }
}
