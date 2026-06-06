package com.merdeleine.gatewaybff.auth.config;

import com.merdeleine.gatewaybff.auth.security.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {


    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }


    // 預設的spring oauth2 login filter會處理 /login/oauth2/code/* 的 callback，所以這裡不需要特別放行該路徑

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        // ✅ CORS preflight 一定要先放行
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        // ✅ OAuth2 登入相關路徑全開放（Spring Security 自動處理）
                        .pathMatchers("/login/**", "/logout", "/oauth2/**").permitAll()

                        // ✅ /auth/me 必須已登入（Session 或 JWT 任一）
                        .pathMatchers("/auth/me").permitAll()

                        // ✅ /auth/register：新用戶攜帶 JWT 完成註冊，無需 Session
                        .pathMatchers("/auth/register").permitAll()

                        // ✅ 內部服務 API（由 X-Internal-Secret header 自行驗證）
                        .pathMatchers("/internal/**").permitAll()

                        // 商品瀏覽開放
                        .pathMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .pathMatchers("/api/catalog/customer/**").permitAll()

                        // LINE webhook 需要公開給 LINE 平台回呼
                        .pathMatchers(HttpMethod.POST, "/api/line/**").permitAll()

                        // 管理後台限 ADMIN
                        .pathMatchers("/api/notification/**").hasRole("ADMIN")
                        .pathMatchers("/api/production-planning/**").hasRole("ADMIN")
                        .pathMatchers("/api/production/**").hasRole("ADMIN")
                        .pathMatchers("/api/threshold/**").hasRole("ADMIN")

                        // 其餘一律要登入
                        .anyExchange().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        // ✅ 登入成功後交給自訂 handler
                        .authenticationSuccessHandler(oAuth2LoginSuccessHandler))   // Google / Facebook OAuth2
                .logout(logout -> logout.logoutUrl("/logout"))

                .build();
    }
}