package com.js.microservices.order_service.infra;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.PostConstruct;

@Configuration @AllArgsConstructor
public class ObservabilityConfig {

    private final KafkaTemplate kafkaTemplate;

    @PostConstruct
    public void setObservationForKafkaTemplate() {
        kafkaTemplate.setObservationEnabled(true);
    }

    @Bean
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }

}
