package com.usdtolkr.currencyconvertor.controller;

import com.usdtolkr.currencyconvertor.dto.CurrencyStatsResponse;
import com.usdtolkr.currencyconvertor.dto.ExchangeRateResponse;
import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import com.usdtolkr.currencyconvertor.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/currency")
public class CurrencyController {
    private final CurrencyService currencyService;

    @GetMapping("/rate")
    public ExchangeRateResponse getRate() {
        return currencyService.getExchangeRate();
    }

    @GetMapping("/stats")
    public CurrencyStatsResponse stats() {
        return currencyService.getStats();
    }

    @PostMapping("/convert")
    public CurrencyLog convertCurrency(
            @RequestParam(name = "usdAmount", required = false) Double usdAmount,
            HttpServletRequest request) {

        if (usdAmount == null) {
            String alt = request.getParameter("usdAmout");
            if (alt != null && !alt.isBlank()) {
                try {
                    usdAmount = Double.valueOf(alt);
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value for 'usdAmount'");
                }
            }
        }

        if (usdAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required request parameter 'usdAmount'");
        }

        return currencyService.convertAndSave(usdAmount);
    }

    @PostMapping("/convert/reverse")
    public CurrencyLog convertReverse(@RequestParam double lkrAmount) {
        return currencyService.convertReverseAndSave(lkrAmount);
    }

    @GetMapping("/history/latest")
    public List<CurrencyLog> latestHistory(@RequestParam(defaultValue = "5") int limit) {
        return currencyService.getLatestHistory(limit);
    }

    @GetMapping("/history/mine")
    public Object myHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return currencyService.getMyHistoryPaged(page, size);
        }
        return currencyService.getMyHistory();
    }

    @GetMapping("/history/count")
    public long historyCount(@RequestParam(defaultValue = "false") boolean mine) {
        return mine ? currencyService.getMyHistoryCount() : currencyService.getHistoryCount();
    }

    @GetMapping("/history/{id}")
    public CurrencyLog historyById(@PathVariable String id) {
        return currencyService.getHistoryById(id);
    }

    @DeleteMapping("/history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable String id) {
        currencyService.deleteHistoryById(id);
    }

    @GetMapping("/history")
    public Object getHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return currencyService.getHistoryPaged(page, size);
        }
        return currencyService.getAllLogs();
    }
}
