package com.usdtolkr.currencyconvertor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConversionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConversionEventConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleCurrencyConversion(CurrencyConversionEvent event) {
        log.info("Received currency conversion event: {}", event);
    }
}
