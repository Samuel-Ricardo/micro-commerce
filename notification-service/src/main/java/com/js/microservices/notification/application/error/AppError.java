package com.js.microservices.notification.application.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Builder @Getter
public class AppError extends RuntimeException {
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
