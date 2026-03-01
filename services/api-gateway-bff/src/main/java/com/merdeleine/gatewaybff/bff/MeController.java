package com.merdeleine.gatewaybff.bff;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/bff/me")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) return Mono.just(Map.of("authenticated", false));

        return Mono.just(Map.of(
                "authenticated", true,
                "name", user.getName(),
                "attributes", user.getAttributes(),
                "authorities", user.getAuthorities().stream().map(Object::toString).toList()
        ));
    }

    @GetMapping("/bff/me2")
    public Map<String, Object> me2(Authentication auth) {
        return Map.of(
                "name", auth.getName(),
                "authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "principalClass", auth.getPrincipal().getClass().getName()
        );
    }
}