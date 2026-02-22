package com.merdeleine.production.planning.config;


import com.merdeleine.production.planning.client.catalog.CatalogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ClientsConfig {

    @Bean(name = "catalogRestClient")
    public RestClient catalogRestClient(
            @Value("${app.clients.catalog.base-url}") String catalogBaseUrl,
            @Value("${app.clients.catalog.connect-timeout:PT2S}") Duration connectTimeout,
            @Value("${app.clients.catalog.read-timeout:PT3S}") Duration readTimeout
    ) {
        // 簡單版：用 default client。若你要更細緻 timeout，可改用 Apache HC5 或 Reactor Netty。
        // 這裡維持「可落地」：大多數內網呼叫夠用。
        return RestClient.builder()
                .baseUrl(catalogBaseUrl)
                .build();
    }

    @Bean
    public CatalogClient catalogClient(RestClient catalogRestClient) {
        return new CatalogClient(catalogRestClient);
    }

    @Bean(name = "orderServiceRestClient")
    RestClient orderServiceRestClient() {
        return RestClient.builder()
                // 你可以用 service discovery 或從 application.yml 讀
                .baseUrl("http://localhost:8083")
                .build();
    }
}