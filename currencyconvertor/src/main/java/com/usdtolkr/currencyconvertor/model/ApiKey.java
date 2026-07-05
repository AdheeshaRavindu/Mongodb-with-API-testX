package com.usdtolkr.currencyconvertor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "api_keys")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiKey {

    @Id
    private String id;
    private String keyValue;
    private String owner;
    private boolean active;
    private String createdAt;
}
