package com.js.microservices.order_service.application.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public abstract class AppError extends RuntimeException {

    private final String message;
    private final HttpStatus status;

    public AppError(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    public String uri() {
        return "https://http.cat/status/" + status.value();
    }
}
