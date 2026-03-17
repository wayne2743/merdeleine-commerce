// 路徑: auth/security/OAuth2LoginSuccessHandler.java
package com.merdeleine.gatewaybff.auth.security;

import com.merdeleine.gatewaybff.auth.repo.AppUserRepository;
import com.merdeleine.gatewaybff.auth.service.JwtService;
import com.merdeleine.gatewaybff.auth.service.UserAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@Component
public class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;
    private final UserAuthService userAuthService;
    private final JwtService jwtService;

    @Value("${app.frontend.redirect-uri}")
    private String frontendRedirectUri;

    @Value("${app.frontend.register-uri:${app.frontend.redirect-uri}/register}")
    private String frontendRegisterUri;

    public OAuth2LoginSuccessHandler(
            AppUserRepository appUserRepository,
            UserAuthService userAuthService,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.userAuthService = userAuthService;
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");

        return Mono.fromCallable(() -> appUserRepository.findByEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return redirect(exchange, frontendRedirectUri + "?error=user_not_found");
                    }

                    var user = optUser.get();
                    return userAuthService.loadRoles(user.getId())
                            .flatMap(roles -> {
                                String token = jwtService.generateToken(user, roles);

                                String targetUrl;
                                if (!user.isProfileCompleted()) {
                                    // 新用戶 → 導向前端註冊頁
                                    targetUrl = UriComponentsBuilder
                                            .fromUriString(frontendRegisterUri)
                                            .queryParam("token", token)
                                            .queryParam("new_user", "true")
                                            .build()
                                            .toUriString();
                                } else {
                                    // 回訪用戶 → 直接進入首頁
                                    targetUrl = UriComponentsBuilder
                                            .fromUriString(frontendRedirectUri)
                                            .queryParam("token", token)
                                            .build()
                                            .toUriString();
                                }

                                return redirect(exchange, targetUrl);
                            });
                });
    }

    private Mono<Void> redirect(WebFilterExchange exchange, String url) {
        var response = exchange.getExchange().getResponse();
        response.getHeaders().setLocation(URI.create(url));
        response.setStatusCode(org.springframework.http.HttpStatus.FOUND); // 302
        return response.setComplete();
    }
}