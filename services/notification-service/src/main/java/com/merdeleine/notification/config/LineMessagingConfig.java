package com.merdeleine.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({LineMessagingProperties.class, LineAccountLinkProperties.class})
public class LineMessagingConfig {

    @Bean("lineRestClient")
    public RestClient lineRestClient(LineMessagingProperties props) {
        return RestClient.builder()
                .baseUrl("https://api.line.me")
                // Token added per-request in LineMessagingService to keep it out of bean-level log output
                .build();
    }
}
