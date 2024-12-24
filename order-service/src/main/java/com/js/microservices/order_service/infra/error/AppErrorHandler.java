package com.js.microservices.order_service.infra.error;


import com.js.microservices.order_service.application.error.AppError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppErrorHandler {

    @ExceptionHandler(AppError.class)
    public ResponseEntity<ErrorResponse> handleAppError(AppError appError) {
        return ResponseEntity
                .status(appError.getStatus())
                .body(ErrorResponse
                        .create(appError, appError.getStatus(), appError.getMessage())
                );
    }

}
