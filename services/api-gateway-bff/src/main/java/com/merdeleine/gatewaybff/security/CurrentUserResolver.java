package com.merdeleine.gatewaybff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CurrentUserResolver {

    @Value("${app.user.header:X-USER-ID}")
    private String header;

    @Value("${app.user.default:dev-user-001}")
    private String defaultUser;

    public String resolve(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(header);
        return StringUtils.hasText(userId) ? userId : defaultUser;
    }

    /**
     * ✅ 非同步版（生產推薦）
     * 優先從 SecurityContext 取 userId（JWT 設置的 principal）
     * 其次從 Header，最後 fallback default
     */
    public Mono<String> resolveReactive(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    var auth = ctx.getAuthentication();
                    if (auth != null && auth.isAuthenticated()) {
                        // JWT filter 設置的 principal 是 userId String
                        if (auth.getPrincipal() instanceof String userId) {
                            return userId;
                        }
                    }
                    return null;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // fallback: header or default
                    String headerVal = exchange.getRequest().getHeaders().getFirst(header);
                    return Mono.just(StringUtils.hasText(headerVal) ? headerVal : defaultUser);
                }))
                .defaultIfEmpty(defaultUser);
    }
}