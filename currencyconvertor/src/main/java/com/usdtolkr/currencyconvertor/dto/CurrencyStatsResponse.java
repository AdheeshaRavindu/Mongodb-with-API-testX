package com.usdtolkr.currencyconvertor.dto;

public class CurrencyStatsResponse {

    private long totalConversions;
    private double totalUsdConverted;
    private double totalLkrOutput;

    public CurrencyStatsResponse() {
    }

    public CurrencyStatsResponse(long totalConversions, double totalUsdConverted, double totalLkrOutput) {
        this.totalConversions = totalConversions;
        this.totalUsdConverted = totalUsdConverted;
        this.totalLkrOutput = totalLkrOutput;
    }

    public long getTotalConversions() {
        return totalConversions;
    }

    public void setTotalConversions(long totalConversions) {
        this.totalConversions = totalConversions;
    }

    public double getTotalUsdConverted() {
        return totalUsdConverted;
    }

    public void setTotalUsdConverted(double totalUsdConverted) {
        this.totalUsdConverted = totalUsdConverted;
    }

    public double getTotalLkrOutput() {
        return totalLkrOutput;
    }

    public void setTotalLkrOutput(double totalLkrOutput) {
        this.totalLkrOutput = totalLkrOutput;
    }
}
