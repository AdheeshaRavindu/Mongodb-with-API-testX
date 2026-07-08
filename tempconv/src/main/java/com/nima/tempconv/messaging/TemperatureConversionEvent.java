package com.nima.tempconv.messaging;

public class TemperatureConversionEvent {

    private String id;
    private double inputTemperature;
    private String inputUnit;
    private double outputTemperature;
    private String outputUnit;
    private String timestamp;
    private String eventType;

    public TemperatureConversionEvent() {
    }

    public TemperatureConversionEvent(
            String id,
            double inputTemperature,
            String inputUnit,
            double outputTemperature,
            String outputUnit,
            String timestamp,
            String eventType) {
        this.id = id;
        this.inputTemperature = inputTemperature;
        this.inputUnit = inputUnit;
        this.outputTemperature = outputTemperature;
        this.outputUnit = outputUnit;
        this.timestamp = timestamp;
        this.eventType = eventType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getInputTemperature() {
        return inputTemperature;
    }

    public void setInputTemperature(double inputTemperature) {
        this.inputTemperature = inputTemperature;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public void setInputUnit(String inputUnit) {
        this.inputUnit = inputUnit;
    }

    public double getOutputTemperature() {
        return outputTemperature;
    }

    public void setOutputTemperature(double outputTemperature) {
        this.outputTemperature = outputTemperature;
    }

    public String getOutputUnit() {
        return outputUnit;
    }

    public void setOutputUnit(String outputUnit) {
        this.outputUnit = outputUnit;
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
        return "TemperatureConversionEvent{"
                + "id='" + id + '\''
                + ", inputTemperature=" + inputTemperature
                + ", inputUnit='" + inputUnit + '\''
                + ", outputTemperature=" + outputTemperature
                + ", outputUnit='" + outputUnit + '\''
                + ", timestamp='" + timestamp + '\''
                + ", eventType='" + eventType + '\''
                + '}';
    }
}
