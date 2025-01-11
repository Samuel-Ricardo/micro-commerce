package com.js.microservices.order_service.application.producer.kafka;

import com.js.microservices.order_service.domain.event.OrderPlacedEvent;
import com.js.microservices.order_service.domain.producer.OrderProducer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
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
        kafka.send(ORDER_PLACED_TOPIC, order);
    }
}
