package com.merdeleine.notification.client;

import com.merdeleine.notification.dto.OrderLookupResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Qualifier("orderServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public OrderLookupResponse getOrder(UUID orderId) {
        return restClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("order-service /orders/{orderId} failed: " + res.getStatusCode());
                })
                .body(OrderLookupResponse.class);
    }
}

