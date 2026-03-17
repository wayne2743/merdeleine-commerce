package com.merdeleine.gatewaybff.auth.security;

import com.merdeleine.gatewaybff.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // 沒有 Bearer Token → 繼續（讓後面的 OAuth2 Session 處理）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            log.warn("Invalid JWT token received");
            return chain.filter(exchange);   // 讓 Security 判定為 401
        }

        try {
            Claims claims = jwtService.parseToken(token);
            String userId = claims.getSubject();                     // UUID string
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .collect(Collectors.toList());

            var authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            log.debug("JWT authenticated userId={}, roles={}", userId, roles);

            // contextWrite → 把 Authentication 放入 Reactor Context
            // Spring Security 的 filter chain 會從 context 讀取它
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }
}