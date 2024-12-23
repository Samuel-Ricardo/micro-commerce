package com.js.microservices.product_service.domain.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document (value = "products")
@Data @Builder
@AllArgsConstructor @NoArgsConstructor
public class ProductModel {
    private String id;
    private String name;
    private String description;
    private String skuCode;
    private BigDecimal price;
}
