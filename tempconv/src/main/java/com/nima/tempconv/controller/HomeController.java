package com.nima.tempconv.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "Temperature Converter API",
                "port", 8081,
                "endpoints", Map.of(
                        "convert", "POST /api/temperatures/convert?value={value}&unit={unit}",
                        "history", "GET /api/temperatures/history",
                        "historyLatest", "GET /api/temperatures/history/latest?limit=5",
                        "historyMine", "GET /api/temperatures/history/mine",
                        "stats", "GET /api/temperatures/stats",
                        "units", "GET /api/temperatures/units",
                        "authMe", "GET /auth/me"),
                "frontend", "Open http://localhost:3000 for the web UI");
    }
}
