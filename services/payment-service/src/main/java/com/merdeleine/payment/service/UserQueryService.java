package com.merdeleine.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class UserQueryService {

    private final RestClient restClient;
    private final String internalSecret;

    public UserQueryService(
            RestClient.Builder restClientBuilder,
            @Value("${services.api-gateway.base-url}") String apiGatewayBaseUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.restClient = restClientBuilder
                .baseUrl(apiGatewayBaseUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    public Map<UUID, CustomerInfo> getCustomerInfosByIds(List<UUID> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> uniqueCustomerIds = new ArrayList<>(new LinkedHashSet<>(customerIds));

        BatchUserLookupResponse response;
        try {
            response = restClient.post()
                    .uri("/internal/users/batch")
                    .header("X-Internal-Secret", internalSecret)
                    .body(new BatchRequest(uniqueCustomerIds))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new RuntimeException("api-gateway /internal/users/batch failed: " + res.getStatusCode());
                    })
                    .body(BatchUserLookupResponse.class);
        } catch (RuntimeException ex) {
            // Some environments may still run an older api-gateway without POST /internal/users/batch.
            if (ex.getMessage() != null && ex.getMessage().contains("405")) {
                return getCustomerInfosBySingleLookup(uniqueCustomerIds);
            }
            throw ex;
        }

        if (response == null || response.users() == null) {
            return Collections.emptyMap();
        }

        Map<UUID, CustomerInfo> result = new LinkedHashMap<>();
        for (UserResponse user : response.users()) {
            if (user.customerId() != null) {
                try {
                    UUID id = UUID.fromString(user.customerId());
                    result.put(id, new CustomerInfo(user.contactName(), user.contactPhone(), user.contactEmail()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    private Map<UUID, CustomerInfo> getCustomerInfosBySingleLookup(List<UUID> customerIds) {
        Map<UUID, CustomerInfo> result = new LinkedHashMap<>();
        for (UUID customerId : customerIds) {
            try {
                UserResponse user = restClient.get()
                        .uri("/internal/users/{customerId}", customerId)
                        .header("X-Internal-Secret", internalSecret)
                        .retrieve()
                        .body(UserResponse.class);

                if (user == null || user.customerId() == null) {
                    continue;
                }

                UUID id = UUID.fromString(user.customerId());
                result.put(id, new CustomerInfo(user.contactName(), user.contactPhone(), user.contactEmail()));
            } catch (HttpClientErrorException.NotFound ignored) {
                // Skip unknown users and continue processing remaining ids.
            } catch (Exception ignored) {
                // Keep enrichment best-effort; PaymentCrudService already degrades gracefully.
            }
        }
        return result;
    }

    public record CustomerInfo(String name, String phone, String email) {
    }

    private record BatchRequest(List<UUID> customerIds) {
    }

    private record UserResponse(
            String customerId,
            String email,
            String displayName,
            String provider,
            String contactName,
            String contactPhone,
            String contactEmail,
            String shippingAddress,
            boolean profileCompleted
    ) {
    }

    private record BatchUserLookupResponse(
            List<UserResponse> users,
            List<String> missingCustomerIds
    ) {
    }
}

