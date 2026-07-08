package com.nima.tempconv.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.nima.tempconv.model.TemperatureLog;

@Component
public class ConversionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ConversionEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public ConversionEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTemperatureConversion(TemperatureLog saved) {
        TemperatureConversionEvent event = new TemperatureConversionEvent(
                saved.getId(),
                saved.getInputTemperature(),
                saved.getInputUnit(),
                saved.getOutputTemperature(),
                saved.getOutputUnit(),
                saved.getTimestamp(),
                "TEMPERATURE_CONVERSION");
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event);
            log.info("Published temperature conversion event for id={}", saved.getId());
        } catch (Exception ex) {
            log.error("Failed to publish temperature conversion event for id={}: {}", saved.getId(), ex.getMessage());
        }
    }
}
