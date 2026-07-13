package com.nima.tempconv.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "conversions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemperatureLog {

    @Id
    private String id;
    private double inputTemperature;
    private String inputUnit;
    private double outputTemperature;
    private String outputUnit;
    private String timestamp;
    private String userId;
}
