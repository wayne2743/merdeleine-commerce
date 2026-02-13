package com.merdeleine.notification.client;


import com.merdeleine.notification.dto.RefsResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CatalogServiceClient {

    private final RestClient restClient;

    public CatalogServiceClient(RestClient catalogServiceRestClient) {
        this.restClient = catalogServiceRestClient;
    }

    public RefsResponse getRefs(UUID productId, UUID sellWindowId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/refs")
                        .queryParam("productId", productId)
                        .queryParam("sellWindowId", sellWindowId)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("catalog-service /internal/refs failed: " + res.getStatusCode());
                })
                .body(RefsResponse.class);
    }
}
