package com.usdtolkr.currencyconvertor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.usdtolkr.currencyconvertor.model.CurrencyLog;

@Component
public class ConversionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ConversionEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public ConversionEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCurrencyConversion(CurrencyLog saved) {
        CurrencyConversionEvent event = new CurrencyConversionEvent(
                saved.getId(),
                saved.getInputAmount(),
                saved.getInputCurrency(),
                saved.getOutputAmount(),
                saved.getOutputCurrency(),
                saved.getExchangeRate(),
                saved.getTimestamp(),
                "CURRENCY_CONVERSION");
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event);
            log.info("Published currency conversion event for id={}", saved.getId());
        } catch (Exception ex) {
            log.error("Failed to publish currency conversion event for id={}: {}", saved.getId(), ex.getMessage());
        }
    }
}
