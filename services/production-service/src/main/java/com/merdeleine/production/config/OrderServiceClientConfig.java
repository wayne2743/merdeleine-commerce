package com.merdeleine.production.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderServiceClientConfig {

    @Bean
    RestClient orderServiceRestClient() {
        return RestClient.builder()
                // 你可以用 service discovery 或從 application.yml 讀
                .baseUrl("http://localhost:8083")
                .build();
    }
}
