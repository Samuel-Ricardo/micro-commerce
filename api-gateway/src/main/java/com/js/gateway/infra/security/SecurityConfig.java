package com.js.gateway.infra.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final String[] FREE_RESOURCE_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/api-docs/**",
            "/aggregate/**",
            "/actuator/prometheus",
            "/aggregate/inventory-service/v3/api-docs",
            "/aggregate/product-service/v3/api-docs",
            "/aggregate/order-service/v3/api-docs"
    };

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity ) throws Exception {
        return httpSecurity.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(FREE_RESOURCE_URLS)
                .permitAll()
                .anyRequest()
                .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}