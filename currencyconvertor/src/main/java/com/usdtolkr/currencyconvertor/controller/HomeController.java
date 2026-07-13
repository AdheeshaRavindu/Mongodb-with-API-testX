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
                        "convertReverse", "POST /api/currency/convert/reverse?lkrAmount={amount}",
                        "history", "GET /api/currency/history",
                        "historyLatest", "GET /api/currency/history/latest?limit=5",
                        "historyMine", "GET /api/currency/history/mine",
                        "stats", "GET /api/currency/stats",
                        "rate", "GET /api/currency/rate"),
                "frontend", "Open http://localhost:3000 for the web UI");
    }
}
