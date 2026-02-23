package com.merdeleine.catalog.config;

import com.merdeleine.catalog.client.OrderQuotaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderClientConfig {

    @Bean
    RestClient orderRestClient(@Value("${app.order.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    OrderQuotaClient orderQuotaClient(RestClient orderRestClient) {
        return new OrderQuotaClient(orderRestClient);
    }
}