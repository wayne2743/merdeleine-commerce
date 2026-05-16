package com.merdeleine.catalog.client;

import com.merdeleine.catalog.dto.threshold.BatchCounterRequest;
import com.merdeleine.catalog.dto.threshold.BatchCounterResponse;
import com.merdeleine.catalog.dto.threshold.BatchCounterLookupRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ThresholdServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ThresholdServiceClient(
            RestTemplate restTemplate,
            @Value("${threshold-service.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 建立 BatchCounter
     */
    public BatchCounterResponse createBatchCounter(BatchCounterRequest request) {

        String url = baseUrl + "/api/batch-counters";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<BatchCounterRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<BatchCounterResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        BatchCounterResponse.class
                );

        return response.getBody();
    }

    /**
     * 查詢 BatchCounter
     */
    public BatchCounterResponse getBatchCounter(String counterId) {

        String url = baseUrl + "/api/batch-counters/{id}";

        return restTemplate.getForObject(
                url,
                BatchCounterResponse.class,
                counterId
        );
    }

    /**
     * 依 sellWindowId 刪除所有 BatchCounter（threshold-service 端 cascade 刪除 counter_event_log）。
     * 供 SellWindowService.delete() 呼叫，確保跨服務資料一致性。
     */
    public void deleteBatchCountersBySellWindowId(String sellWindowId) {
        String url = baseUrl + "/api/batch-counters/by-sell-window/" + sellWindowId;
        restTemplate.delete(url);
    }

    public List<BatchCounterResponse> queryBatchCounters(List<BatchCounterLookupRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        String url = baseUrl + "/api/batch-counters/query";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BatchCounterLookupRequest> entity = new HttpEntity<>(new BatchCounterLookupRequest(items), headers);

        ResponseEntity<List<BatchCounterResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody() == null ? List.of() : response.getBody();
    }
}
