package com.merdeleine.catalog.client;

import com.merdeleine.catalog.dto.threshold.BatchCounterRequest;
import com.merdeleine.catalog.dto.threshold.BatchCounterResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
}
