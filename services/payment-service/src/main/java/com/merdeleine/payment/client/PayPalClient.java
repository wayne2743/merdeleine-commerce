package com.merdeleine.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.merdeleine.payment.config.PayPalProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class PayPalClient {

    private final PayPalProperties properties;

    public PayPalClient(PayPalProperties properties) {
        this.properties = properties;
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    public String getAccessToken() {
        String basicAuth = Base64.getEncoder().encodeToString(
                (properties.clientId() + ":" + properties.clientSecret())
                        .getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        JsonNode response = restClient()
                .post()
                .uri("/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic " + basicAuth)
                .body(formData)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("無法取得 PayPal access token");
        }

        return response.get("access_token").asText();
    }

    public JsonNode createOrder(String accessToken, Map<String, Object> body) {
        return restClient()
                .post()
                .uri("/v2/checkout/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("Prefer", "return=representation")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode captureOrder(String accessToken, String orderId) {
        return restClient()
                .post()
                .uri("/v2/checkout/orders/{orderId}/capture", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .header("Prefer", "return=representation")
                .retrieve()
                .body(JsonNode.class);
    }
}