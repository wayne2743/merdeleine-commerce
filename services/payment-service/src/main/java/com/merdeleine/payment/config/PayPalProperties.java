package com.merdeleine.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paypal")
public record PayPalProperties(
        String baseUrl,
        String clientId,
        String clientSecret
) {
}