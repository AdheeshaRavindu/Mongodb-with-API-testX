package com.nima.tempconv.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nima.tempconv.dto.PagedResponse;
import com.nima.tempconv.dto.TemperatureStatsResponse;
import com.nima.tempconv.model.TemperatureLog;
import com.nima.tempconv.service.TemperatureService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/temperatures")
public class TemperatureController {

    private final TemperatureService temperatureService;

    public TemperatureController(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }

    @PostMapping("/convert")
    public TemperatureLog convert(
            @RequestParam double value,
            @RequestParam String unit) {
        return temperatureService.convertAndSave(value, unit);
    }

    @GetMapping(value = "/safety-check", produces = MediaType.TEXT_PLAIN_VALUE)
    public String safetyCheck(@RequestParam double value, @RequestParam String unit) {
        return temperatureService.getSafetyWarning(value, unit);
    }

    @GetMapping("/units")
    public List<String> units() {
        return temperatureService.getSupportedUnits();
    }

    @GetMapping("/stats")
    public TemperatureStatsResponse stats() {
        return temperatureService.getStats();
    }

    @GetMapping("/history/latest")
    public List<TemperatureLog> latestHistory(@RequestParam(defaultValue = "5") int limit) {
        return temperatureService.getLatestHistory(limit);
    }

    @GetMapping("/history/mine")
    public Object myHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return temperatureService.getMyHistoryPaged(page, size);
        }
        return temperatureService.getMyHistory();
    }

    @GetMapping("/history/count")
    public long historyCount(@RequestParam(defaultValue = "false") boolean mine) {
        return mine ? temperatureService.getMyHistoryCount() : temperatureService.getHistoryCount();
    }

    @GetMapping("/history/filter")
    public List<TemperatureLog> historyFilter(@RequestParam String unit) {
        return temperatureService.getHistoryByUnit(unit);
    }

    @GetMapping("/history/{id}")
    public TemperatureLog historyById(@PathVariable String id) {
        return temperatureService.getHistoryById(id);
    }

    @DeleteMapping("/history/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable String id) {
        temperatureService.deleteHistoryById(id);
    }

    @GetMapping("/history")
    public Object history(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null) {
            return temperatureService.getHistoryPaged(page, size);
        }
        return temperatureService.getHistory();
    }
}
