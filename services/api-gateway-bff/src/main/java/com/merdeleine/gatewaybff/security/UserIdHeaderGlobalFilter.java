package com.merdeleine.gatewaybff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Injects the authenticated user's id into the {@code X-USER-ID} header for
 * every request proxied to a downstream service.
 *
 * <p>Local BFF controllers read the identity from the {@code SecurityContext}
 * via {@link CurrentUserResolver}, but routed services (e.g. notification-service)
 * only receive what the gateway forwards. Without this filter a downstream
 * controller reading {@code X-USER-ID} would never see the member id.
 *
 * <p>Any client-supplied {@code X-USER-ID} is stripped first so the value can
 * never be spoofed; it is re-added only from the verified principal.
 */
@Component
public class UserIdHeaderGlobalFilter implements GlobalFilter, Ordered {

    private static final String ANONYMOUS = "anonymousUser";

    @Value("${app.user.header:X-USER-ID}")
    private String userHeader;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1) 先移除 client 自帶的 header，防止偽造身分
        ServerWebExchange sanitized = exchange.mutate()
                .request(r -> r.headers(h -> h.remove(userHeader)))
                .build();

        // 2) 從 SecurityContext 取已驗證的 userId，注入下游請求
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(this::isRealUser)
                .map(auth -> (String) auth.getPrincipal())
                .flatMap(userId -> chain.filter(sanitized.mutate()
                        .request(r -> r.header(userHeader, userId))
                        .build()))
                .switchIfEmpty(chain.filter(sanitized));
    }

    private boolean isRealUser(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof String principal
                && !ANONYMOUS.equals(principal);
    }

    @Override
    public int getOrder() {
        // 在 routing filter（LOWEST_PRECEDENCE）之前執行，確保 header 被帶到下游
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
