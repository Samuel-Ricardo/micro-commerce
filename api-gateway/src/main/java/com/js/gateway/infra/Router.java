package com.js.gateway.infra;

import org.springframework.beans.factory.annotation.Value;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;

@Configuration
public class Router {

    @Value("${services.url.product}")
    private String productServiceUrl;

    @Value("${services.url.order}")
    private String orderServiceUrl;

    @Value("${services.url.inventory}")
    private String inventoryServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> productService() {
        return GatewayRouterFunctions.route("product_service")
                .route(RequestPredicates.path("/api/v1/products/**"), http(productServiceUrl))
                .build();
    }

    
}
