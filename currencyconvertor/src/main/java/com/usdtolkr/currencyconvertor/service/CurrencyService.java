package com.usdtolkr.currencyconvertor.service;

import com.usdtolkr.currencyconvertor.Repository.CurrencyRepository;
import com.usdtolkr.currencyconvertor.dto.CurrencyStatsResponse;
import com.usdtolkr.currencyconvertor.dto.ExchangeRateResponse;
import com.usdtolkr.currencyconvertor.dto.PagedResponse;
import com.usdtolkr.currencyconvertor.exception.ResourceNotFoundException;
import com.usdtolkr.currencyconvertor.messaging.ConversionEventProducer;
import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import com.usdtolkr.currencyconvertor.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final ConversionEventProducer conversionEventProducer;
    private final CurrentUserService currentUserService;

    public static final double USD_TO_LKR_RATE = 300.0;

    public ExchangeRateResponse getExchangeRate() {
        return new ExchangeRateResponse("USD", "LKR", USD_TO_LKR_RATE);
    }

    public CurrencyLog convertAndSave(double amount) {
        return saveConversion(amount, "USD", amount * USD_TO_LKR_RATE, "LKR", USD_TO_LKR_RATE);
    }

    public CurrencyLog convertReverseAndSave(double lkrAmount) {
        double usdAmount = lkrAmount / USD_TO_LKR_RATE;
        return saveConversion(lkrAmount, "LKR", usdAmount, "USD", 1.0 / USD_TO_LKR_RATE);
    }

    private CurrencyLog saveConversion(
            double inputAmount,
            String inputCurrency,
            double outputAmount,
            String outputCurrency,
            double exchangeRate) {
        CurrencyLog log = new CurrencyLog();
        log.setInputAmount(inputAmount);
        log.setInputCurrency(inputCurrency);
        log.setOutputAmount(outputAmount);
        log.setOutputCurrency(outputCurrency);
        log.setExchangeRate(exchangeRate);
        log.setTimestamp(LocalDateTime.now().toString());
        log.setUserId(currentUserService.requireUserId());

        CurrencyLog saved = currencyRepository.save(log);
        conversionEventProducer.publishCurrencyConversion(saved);
        return saved;
    }

    public List<CurrencyLog> getAllLogs() {
        return currencyRepository.findAll();
    }

    public PagedResponse<CurrencyLog> getHistoryPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<CurrencyLog> result = currencyRepository.findAll(pageable);
        return toPagedResponse(result);
    }

    public List<CurrencyLog> getLatestHistory(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return currencyRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public List<CurrencyLog> getMyHistory() {
        String userId = currentUserService.requireUserId();
        return currencyRepository.findByUserIdOrderByTimestampDesc(
                userId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    public PagedResponse<CurrencyLog> getMyHistoryPaged(int page, int size) {
        String userId = currentUserService.requireUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<CurrencyLog> result = currencyRepository.findByUserId(userId, pageable);
        return toPagedResponse(result);
    }

    public long getHistoryCount() {
        return currencyRepository.count();
    }

    public long getMyHistoryCount() {
        return currencyRepository.countByUserId(currentUserService.requireUserId());
    }

    public CurrencyLog getHistoryById(String id) {
        return currencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency conversion not found: " + id));
    }

    public void deleteHistoryById(String id) {
        if (!currencyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Currency conversion not found: " + id);
        }
        currencyRepository.deleteById(id);
    }

    public CurrencyStatsResponse getStats() {
        List<CurrencyLog> logs = currencyRepository.findAll();
        double totalUsd = 0;
        double totalLkr = 0;
        for (CurrencyLog log : logs) {
            if ("USD".equalsIgnoreCase(log.getInputCurrency())) {
                totalUsd += log.getInputAmount();
                totalLkr += log.getOutputAmount();
            } else if ("LKR".equalsIgnoreCase(log.getInputCurrency())) {
                totalLkr += log.getInputAmount();
                totalUsd += log.getOutputAmount();
            }
        }
        return new CurrencyStatsResponse(logs.size(), totalUsd, totalLkr);
    }

    private PagedResponse<CurrencyLog> toPagedResponse(Page<CurrencyLog> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
