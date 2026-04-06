package com.merdeleine.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CatalogQueryService {

    private final RestClient restClient;

    public CatalogQueryService(
            RestClient.Builder restClientBuilder,
            @Value("${services.catalog-service.base-url}") String catalogServiceBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(catalogServiceBaseUrl)
                .build();
    }

    public String getSellWindowName(UUID sellWindowId) {
        SellWindowResponse response = restClient.get()
                .uri("/sell-windows/{sellWindowId}", sellWindowId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("catalog-service /sell-windows/{sellWindowId} failed: " + res.getStatusCode());
                })
                .body(SellWindowResponse.class);

        if (response == null) {
            throw new IllegalArgumentException("SellWindow not found: " + sellWindowId);
        }

        return response.name();
    }

    public Map<UUID, String> getSellWindowNamesByIds(List<UUID> sellWindowIds) {
        if (sellWindowIds == null || sellWindowIds.isEmpty()) {
            return Map.of();
        }

        List<SellWindowNameRefResponse> refs = restClient.post()
                .uri("/internal/sell-windows/_batch-names")
                .body(new BatchSellWindowNameRequest(sellWindowIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new RuntimeException("catalog-service /internal/sell-windows/_batch-names failed: " + res.getStatusCode());
                })
                .body(new ParameterizedTypeReference<List<SellWindowNameRefResponse>>() {});

        if (refs == null || refs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, String> result = new LinkedHashMap<>();
        for (SellWindowNameRefResponse ref : refs) {
            result.put(ref.sellWindowId(), ref.sellWindowName());
        }
        return result;
    }

    private record SellWindowResponse(
            UUID id,
            String name
    ) {
    }

    private record BatchSellWindowNameRequest(List<UUID> sellWindowIds) {
    }

    private record SellWindowNameRefResponse(UUID sellWindowId, String sellWindowName) {
    }
}

