package com.usdtolkr.currencyconvertor.controller;

import com.usdtolkr.currencyconvertor.model.CurrencyLog;
import com.usdtolkr.currencyconvertor.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/convert")
    public CurrencyLog convertCurrency(
            @RequestParam(name = "usdAmount", required = false) Double usdAmount,
            HttpServletRequest request) {

        // tolerate a common typo that was seen in Postman: "usdAmout"
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

    @GetMapping("/history")
    public List<CurrencyLog> getHistory() {
        return currencyService.getAllLogs();
    }
}
