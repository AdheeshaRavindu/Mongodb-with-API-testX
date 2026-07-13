package com.nima.tempconv.dto;

import java.util.Map;

public class TemperatureStatsResponse {

    private long totalConversions;
    private Map<String, Long> byUnit;

    public TemperatureStatsResponse() {
    }

    public TemperatureStatsResponse(long totalConversions, Map<String, Long> byUnit) {
        this.totalConversions = totalConversions;
        this.byUnit = byUnit;
    }

    public long getTotalConversions() {
        return totalConversions;
    }

    public void setTotalConversions(long totalConversions) {
        this.totalConversions = totalConversions;
    }

    public Map<String, Long> getByUnit() {
        return byUnit;
    }

    public void setByUnit(Map<String, Long> byUnit) {
        this.byUnit = byUnit;
    }
}
