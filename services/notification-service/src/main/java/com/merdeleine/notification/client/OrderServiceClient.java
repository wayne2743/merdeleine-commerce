package com.merdeleine.notification.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.notification.dto.OrderLookupResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class OrderServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OrderServiceClient(@Qualifier("orderServiceRestClient") RestClient restClient,
                              ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public OrderLookupResponse getOrder(UUID orderId) {
        JsonNode response = restClient.get()
                .uri("/orders/{orderId}", orderId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("order-service /orders/{orderId} failed: " + res.getStatusCode());
                })
                .body(JsonNode.class);

        if (response == null || response.isNull()) {
            return null;
        }

        JsonNode orderNode = response.path("data").isObject() ? response.path("data") : response;

        // Some environments may route this path to payment payloads that do not contain order refs.
        if (!orderNode.hasNonNull("productId") && !orderNode.hasNonNull("sellWindowId")) {
            return null;
        }

        return objectMapper.convertValue(orderNode, OrderLookupResponse.class);
    }
}

