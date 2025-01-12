package com.js.microservices.notification.application.consumer.kafka;

import com.js.microservices.notification.domain.consumer.OrderConsumer;
import com.js.microservices.notification.domain.dto.SendOrderPlacedMailNotificationDTO;
import com.js.microservices.notification.domain.service.NotificationService;
import com.js.microservices.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class KafkaOrderConsumer implements OrderConsumer {

    private final NotificationService service;

    @Override @KafkaListener(topics = "${kafka.topic.order.placed}")
    public void listenOrderPlacedEvent(@NotNull OrderPlacedEvent event) {
        var order = SendOrderPlacedMailNotificationDTO.builder()
            .orderNumber(event.getOrderNumber().toString())
            .to(event.getEmail().toString())
            .firstName(event.getFirstName().toString())
            .lastName(event.getLastName().toString())
            .build();

            service.sendOrderPlaced(order);
    }
}
