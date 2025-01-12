package com.js.microservices.notification.domain.dto;

import lombok.Builder;
import lombok.Data;

@Builder @Data
public class FormatMailBodyDTO {
    private String orderNumber;
    private String firstName;
    private String lastName;
}
