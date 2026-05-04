package com.merdeleine.gatewaybff.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.authentication.ReactiveOidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

@Configuration
public class OAuth2OidcConfig {

    @Bean
    public ReactiveOidcIdTokenDecoderFactory reactiveOidcIdTokenDecoderFactory() {
        ReactiveOidcIdTokenDecoderFactory factory = new ReactiveOidcIdTokenDecoderFactory();

        factory.setJwsAlgorithmResolver(clientRegistration -> {
            if ("line".equals(clientRegistration.getRegistrationId())) {
                return MacAlgorithm.HS256;
            }

            // Google / 其他 OIDC provider 用預設 RS256
            return SignatureAlgorithm.RS256;
        });

        return factory;
    }
}