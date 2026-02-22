package com.merdeleine.production.planning.client.catalog;

import com.merdeleine.production.planning.dto.OpenPaymentRequest;
import com.merdeleine.production.planning.dto.OpenPaymentResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.util.UUID;


public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Qualifier(value = "catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public OpenPaymentResponse openPayment(UUID sellWindowId, OpenPaymentRequest request) {
        // request 可以傳 null（Catalog 端有 @RequestBody(required=false) 的話）
        return restClient.post()
                .uri("/internal/sell-windows/{id}/open-payment", sellWindowId)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    int code = res.getStatusCode().value();
                    String body = safeBody(res);
                    if (code == 404) throw new CatalogClientNotFoundException("Catalog sellWindow not found: " + sellWindowId);
                    if (code == 409) throw new CatalogClientConflictException("Catalog conflict open-payment: " + body);
                    if (code == 400) throw new CatalogClientBadRequestException("Catalog bad request open-payment: " + body);
                    throw new CatalogClientException("Catalog 4xx open-payment (" + code + "): " + body);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    String body = safeBody(res);
                    throw new CatalogClientServerException("Catalog 5xx open-payment: " + body);
                })
                .body(OpenPaymentResponse.class);
    }

    private static String safeBody(ClientHttpResponse res) {
        try {
            // Spring 6: response body 只能讀一次；這裡只做 best-effort
            return new String(res.getBody().readAllBytes());
        } catch (Exception e) {
            return "<unreadable>";
        }
    }
}
