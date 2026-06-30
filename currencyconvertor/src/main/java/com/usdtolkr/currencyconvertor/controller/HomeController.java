package com.usdtolkr.currencyconvertor.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "Currency Converter API",
                "port", 8082,
                "endpoints", Map.of(
                        "convert", "POST /api/currency/convert?usdAmount={amount}",
                        "history", "GET /api/currency/history"),
                "frontend", "Open http://localhost:3000 for the web UI");
    }
}
