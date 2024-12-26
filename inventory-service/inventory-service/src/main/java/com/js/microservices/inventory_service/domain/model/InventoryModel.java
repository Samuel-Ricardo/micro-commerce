package com.js.microservices.inventory_service.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

@Entity @Table(name = "t_inventory")
@Data @Builder
public class InventoryModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String skuCode;
    private Integer quantity;
}
