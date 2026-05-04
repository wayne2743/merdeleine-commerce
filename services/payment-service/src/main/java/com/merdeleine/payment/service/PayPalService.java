package com.merdeleine.payment.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.merdeleine.payment.client.PayPalClient;
import com.merdeleine.payment.dto.CaptureOrderResponse;
import com.merdeleine.payment.dto.CreateOrderRequest;
import com.merdeleine.payment.dto.CreateOrderResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service

public class PayPalService {

    private final PayPalClient payPalClient;

    public PayPalService(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }


    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String accessToken = payPalClient.getAccessToken();

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(
                        Map.of(
                                "amount", Map.of(
                                        "currency_code", request.currency(),
                                        "value", request.amount()
                                )
                        )
                )
        );

        JsonNode response = payPalClient.createOrder(accessToken, body);

        return new CreateOrderResponse(
                response.path("id").asText(),
                response.path("status").asText()
        );
    }

    public CaptureOrderResponse captureOrder(String orderId) {
        String accessToken = payPalClient.getAccessToken();
        JsonNode response = payPalClient.captureOrder(accessToken, orderId);

        return new CaptureOrderResponse(
                response.path("id").asText(),
                response.path("status").asText(),
                response
        );
    }
}