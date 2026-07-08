package com.usdtolkr.currencyconvertor.messaging;

public class CurrencyConversionEvent {

    private String id;
    private double inputAmount;
    private String inputCurrency;
    private double outputAmount;
    private String outputCurrency;
    private double exchangeRate;
    private String timestamp;
    private String eventType;

    public CurrencyConversionEvent() {
    }

    public CurrencyConversionEvent(
            String id,
            double inputAmount,
            String inputCurrency,
            double outputAmount,
            String outputCurrency,
            double exchangeRate,
            String timestamp,
            String eventType) {
        this.id = id;
        this.inputAmount = inputAmount;
        this.inputCurrency = inputCurrency;
        this.outputAmount = outputAmount;
        this.outputCurrency = outputCurrency;
        this.exchangeRate = exchangeRate;
        this.timestamp = timestamp;
        this.eventType = eventType;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "CurrencyConversionEvent{"
                + "id='" + id + '\''
                + ", inputAmount=" + inputAmount
                + ", inputCurrency='" + inputCurrency + '\''
                + ", outputAmount=" + outputAmount
                + ", outputCurrency='" + outputCurrency + '\''
                + ", exchangeRate=" + exchangeRate
                + ", timestamp='" + timestamp + '\''
                + ", eventType='" + eventType + '\''
                + '}';
    }
}
