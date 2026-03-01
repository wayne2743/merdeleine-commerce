package com.merdeleine.gatewaybff.bff;

import com.merdeleine.gatewaybff.security.CurrentUserResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff")
public class OrderBffController {

    private final WebClient webClient;
    private final CurrentUserResolver currentUserResolver;

    // 這個 baseUrl 先用 @Value，避免你還沒做 @ConfigurationProperties
    private final String orderBaseUrl;

    public OrderBffController(
            WebClient webClient,
            CurrentUserResolver currentUserResolver,
            @org.springframework.beans.factory.annotation.Value("${app.services.order.base-url}") String orderBaseUrl
    ) {
        this.webClient = webClient;
        this.currentUserResolver = currentUserResolver;
        this.orderBaseUrl = orderBaseUrl;
    }

    @GetMapping(value = "/my-orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Map<String, Object>>> myOrders(ServerWebExchange exchange) {
        String userId = currentUserResolver.resolve(exchange);

        // 假設 order-service: GET /orders?customerId=...
        return webClient.get()
                .uri(orderBaseUrl + "/orders?customerId=" + userId)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .cast((Class<List<Map<String, Object>>>) (Class<?>) List.class);
    }

    @PostMapping(value = "/orders/reserve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> reserve(@RequestBody Map<String, Object> req, ServerWebExchange exchange) {
        String userId = currentUserResolver.resolve(exchange);

        Map<String, Object> payload = Map.of(
                "sellWindowId", req.get("sellWindowId"),
                "productId", req.get("productId"),
                "quantity", req.get("quantity"),
                "customerId", userId,
                "status", "RESERVED"
        );

        return webClient.post()
                .uri(orderBaseUrl + "/orders")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .cast((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
}