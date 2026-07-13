package com.usdtolkr.currencyconvertor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class CurrencyLog {
    @Id
    private String id;
    private double inputAmount;
    private String inputCurrency;
    private double outputAmount;
    private String outputCurrency;
    private double exchangeRate;
    private String timestamp;
    private String userId;

    public CurrencyLog() {
    }

    public CurrencyLog(String id, double inputAmount, String inputCurrency, double outputAmount, String outputCurrency, double exchangeRate, String timestamp) {
        this.id = id;
        this.inputAmount = inputAmount;
        this.inputCurrency = inputCurrency;
        this.outputAmount = outputAmount;
        this.outputCurrency = outputCurrency;
        this.exchangeRate = exchangeRate;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getInputAmount() {
        return inputAmount;
    }

    public void setInputAmount(double inputAmount) {
        this.inputAmount = inputAmount;
    }

    public String getInputCurrency() {
        return inputCurrency;
    }

    public void setInputCurrency(String inputCurrency) {
        this.inputCurrency = inputCurrency;
    }

    public double getOutputAmount() {
        return outputAmount;
    }

    public void setOutputAmount(double outputAmount) {
        this.outputAmount = outputAmount;
    }

    public String getOutputCurrency() {
        return outputCurrency;
    }

    public void setOutputCurrency(String outputCurrency) {
        this.outputCurrency = outputCurrency;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
