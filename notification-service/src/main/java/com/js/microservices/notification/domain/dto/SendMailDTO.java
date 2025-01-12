package com.js.microservices.notification.domain.dto;

import lombok.Builder;
import lombok.Data;


@Builder @Data
public class SendMailDTO {
    private String to;
    private String subject;
    private String body;
}
