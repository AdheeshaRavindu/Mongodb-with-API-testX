package com.converthub.gateway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayInfoController {

    @GetMapping("/")
    public Map<String, Object> info() {
        return Map.of(
                "service", "ConvertHub API Gateway",
                "port", 8080,
                "routes", List.of(
                        "POST /auth/google -> tempconv",
                        "GET /auth/me -> tempconv",
                        "/api/temperatures/** -> tempconv",
                        "/api/currency/** -> currencyconvertor"),
                "frontend", "Open http://localhost:3000 for the web UI",
                "note", "All /api/** routes require Authorization: Bearer <application_jwt>");
    }
}
