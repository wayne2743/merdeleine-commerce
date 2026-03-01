package com.merdeleine.gatewaybff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

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
}