package com.merdeleine.gatewaybff.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(ex -> ex
                        // actuator 你要不要放開看你；內網開發先放開
                        .pathMatchers("/actuator/**").permitAll()

                        // 例如：商品瀏覽先開放
                        .pathMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .pathMatchers("/api/notification/**").hasRole("ADMIN")
                        .pathMatchers("/api/production-planning/**").hasRole("ADMIN")
                        .pathMatchers("/api/production/**").hasRole("ADMIN")
                        .pathMatchers("/api/threshold/**").hasRole("ADMIN")
                        // 其餘一律要登入
                        .anyExchange().authenticated()
                )

                .oauth2Login(oauth2 -> { })
                .logout(logout -> logout
                        // 你也可以做 /logout 後導回首頁
                        .logoutUrl("/logout")
                )

                // 預設用 WebSession 存登入狀態（BFF 常用）
                .build();
    }
}