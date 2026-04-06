package com.merdeleine.gatewaybff.internal;

import com.merdeleine.gatewaybff.auth.dto.BatchUserLookupResponse;
import com.merdeleine.gatewaybff.auth.dto.UserLookupResponse;
import com.merdeleine.gatewaybff.auth.entity.AppUser;
import com.merdeleine.gatewaybff.auth.service.UserAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 內部服務專用 API，不經過 OAuth 驗證。
 * 呼叫方必須在 Header 帶上 X-Internal-Secret 才能存取。
 */
@RestController
@RequestMapping("/internal")
public class InternalUserController {

    private final UserAuthService userAuthService;
    private final String internalSecret;

    public InternalUserController(
            UserAuthService userAuthService,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.userAuthService = userAuthService;
        this.internalSecret = internalSecret;
    }

    /**
     * GET /internal/users/{customerId}
     * 供後端服務（如 notification-service）查詢用戶資料，不需 OAuth。
     */
    @GetMapping("/users/{customerId}")
    public Mono<ResponseEntity<UserLookupResponse>> getUserByCustomerId(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @PathVariable UUID customerId
    ) {
        if (!internalSecret.equals(secret)) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        return userAuthService.findByCustomerId(customerId)
                .map(optUser -> optUser
                        .map(user -> ResponseEntity.ok(toUserLookupResponse(user)))
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    /**
     * POST /internal/users/batch
     * 批次查詢多個 customerId 的用戶資料，供後端服務使用，不需 OAuth。
     */
    @PostMapping("/users/batch")
    public Mono<ResponseEntity<BatchUserLookupResponse>> batchGetUsers(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody(required = false) BatchRequest request
    ) {
        if (!internalSecret.equals(secret)) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        List<UUID> customerIds = request == null || request.customerIds() == null
                ? List.of()
                : request.customerIds();

        if (customerIds.isEmpty()) {
            return Mono.just(ResponseEntity.ok(new BatchUserLookupResponse(List.of(), List.of())));
        }

        return userAuthService.findByCustomerIds(customerIds)
                .map(users -> {
                    Set<UUID> foundIds = users.stream()
                            .map(AppUser::getId)
                            .collect(Collectors.toSet());

                    List<String> missingCustomerIds = customerIds.stream()
                            .distinct()
                            .filter(id -> !foundIds.contains(id))
                            .map(UUID::toString)
                            .toList();

                    List<UserLookupResponse> responses = users.stream()
                            .map(this::toUserLookupResponse)
                            .toList();

                    return ResponseEntity.ok(new BatchUserLookupResponse(responses, missingCustomerIds));
                });
    }

    private UserLookupResponse toUserLookupResponse(AppUser user) {
        return new UserLookupResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProvider(),
                user.getContactName(),
                user.getContactPhone(),
                user.getContactEmail(),
                user.getShippingAddress(),
                user.isProfileCompleted()
        );
    }

    public record BatchRequest(List<UUID> customerIds) {
    }
}

