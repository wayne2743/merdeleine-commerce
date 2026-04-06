package com.merdeleine.notification.client;

import com.merdeleine.notification.dto.UserLookupResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class ApiGatewayClient {

    private final RestClient restClient;
    private final String internalSecret;

    public ApiGatewayClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.api-gateway.base-url}") String baseUrl,
            @Value("${services.api-gateway.timeout-ms}") long timeoutMs,
            @Value("${services.api-gateway.internal-secret}") String internalSecret
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.internalSecret = internalSecret;
    }

    public UserLookupResponse getUserByCustomerId(UUID customerId) {
        return restClient.get()
                .uri("/internal/users/{customerId}", customerId)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("api-gateway /internal/users/{customerId} failed: " + res.getStatusCode());
                })
                .body(UserLookupResponse.class);
    }
}




