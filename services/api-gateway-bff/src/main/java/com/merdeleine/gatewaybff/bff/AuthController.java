package com.merdeleine.gatewaybff.bff;

import com.merdeleine.gatewaybff.auth.dto.CompleteRegistrationRequest;
import com.merdeleine.gatewaybff.auth.dto.MeResponse;
import com.merdeleine.gatewaybff.auth.dto.UpdateUserProfileRequest;
import com.merdeleine.gatewaybff.auth.entity.AppUser;
import com.merdeleine.gatewaybff.auth.repo.AppUserRepository;
import com.merdeleine.gatewaybff.auth.service.JwtService;
import com.merdeleine.gatewaybff.auth.service.UserAuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final UserAuthService userAuthService;
    private final JwtService jwtService;

    public AuthController(
            AppUserRepository appUserRepository,
            UserAuthService userAuthService,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.userAuthService = userAuthService;
        this.jwtService = jwtService;
    }

    /**
     * GET /auth/me
     * 前置條件：已透過 OAuth2 登入（持有 Session Cookie）
     * 回傳：目前登入的使用者資訊 + JWT token
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<MeResponse>> me(
            @AuthenticationPrincipal OAuth2User principal
    ) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            return Mono.just(ResponseEntity.status(401).<MeResponse>build());
        }

        return Mono.fromCallable(() -> appUserRepository.findByEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return Mono.just(ResponseEntity.<MeResponse>status(404).build());
                    }
                    AppUser user = optUser.get();
                    return userAuthService.loadRoles(user.getId())
                            .map(roles -> {
                                String token = jwtService.generateToken(user, roles);
                                List<String> roleCodes = roles.stream()
                                        .map(r -> r.getCode())
                                        .collect(Collectors.toList());
                                return ResponseEntity.ok(toMeResponse(user, roleCodes, token));
                            });
                });
    }

    /**
     * POST /auth/register
     * 新用戶 Google OAuth2 驗證後，填寫聯絡資訊完成帳號註冊
     * 需帶 Authorization: Bearer <jwt>（OAuth2 登入成功後取得的 token）
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<MeResponse>> register(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CompleteRegistrationRequest req
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        UUID userId = UUID.fromString(claims.getSubject());

        return userAuthService.completeRegistration(userId, req)
                .flatMap(user -> userAuthService.loadRoles(user.getId())
                        .map(roles -> {
                            String newToken = jwtService.generateToken(user, roles);
                            List<String> roleCodes = roles.stream()
                                    .map(r -> r.getCode())
                                    .collect(Collectors.toList());
                            return ResponseEntity.ok(toMeResponse(user, roleCodes, newToken));
                        }));
    }

    /**
     * PUT /auth/me
     * 更新已完成註冊的使用者聯絡資訊
     */
    @PutMapping("/me")
    public Mono<ResponseEntity<MeResponse>> updateProfile(
            @AuthenticationPrincipal OAuth2User principal,
            @Valid @RequestBody UpdateUserProfileRequest req
    ) {
        if (principal == null) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            return Mono.just(ResponseEntity.status(401).<MeResponse>build());
        }

        return userAuthService.updateProfile(email, req)
                .flatMap(user -> userAuthService.loadRoles(user.getId())
                        .map(roles -> {
                            String newToken = jwtService.generateToken(user, roles);
                            List<String> roleCodes = roles.stream()
                                    .map(r -> r.getCode())
                                    .collect(Collectors.toList());
                            return ResponseEntity.ok(toMeResponse(user, roleCodes, newToken));
                        }));
    }

    private MeResponse toMeResponse(AppUser user, List<String> roleCodes, String token) {
        return new MeResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProvider(),
                roleCodes,
                token,
                user.getContactName(),
                user.getContactPhone(),
                user.getContactEmail(),
                user.getShippingAddress()
        );
    }
}