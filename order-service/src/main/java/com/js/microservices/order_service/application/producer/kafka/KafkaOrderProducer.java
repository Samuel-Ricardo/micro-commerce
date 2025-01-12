package com.js.microservices.order_service.application.producer.kafka;

import com.js.microservices.order.event.OrderPlacedEvent;
import com.js.microservices.order_service.domain.producer.OrderProducer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component @Slf4j
public class KafkaOrderProducer implements OrderProducer {

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafka;

    @Value("${kafka.topic.order.placed}")
    private String ORDER_PLACED_TOPIC;

    

    /**
     * @param order
     */
    @Override
    public void produceOrderPlacedEvent(@NotNull OrderPlacedEvent order) {
        log.info("Start - Sending Order Placed Event {} to Kafka topic: {} ", order, ORDER_PLACED_TOPIC);
        kafka.send(ORDER_PLACED_TOPIC, order);
        log.info("End - Sending Order Placed Event {} to Kafka topic: {}", order, ORDER_PLACED_TOPIC);
    }
}
