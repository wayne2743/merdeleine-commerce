package com.merdeleine.gatewaybff.auth.security;

import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.repo.AppUserRepository;
import com.merdeleine.gatewaybff.auth.service.UserAuthService;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary // ✅ 關鍵：讓 Spring Security OAuth2 Login 用你這支
public class DbBackedReactiveOAuth2UserService
        implements ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DbBackedReactiveOAuth2UserService.class);
    private final DefaultReactiveOAuth2UserService delegate = new DefaultReactiveOAuth2UserService();

    private final AppUserRepository appUserRepository;
    private final UserAuthService userAuthService;

    public DbBackedReactiveOAuth2UserService(
            AppUserRepository appUserRepository,
            UserAuthService userAuthService
    ) {
        this.appUserRepository = appUserRepository;
        this.userAuthService = userAuthService;
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Loading user {}", userRequest.getClientRegistration().getRegistrationId());
        return delegate.loadUser(userRequest)
                .flatMap(oauthUser -> upsertAndBuildPrincipal(userRequest, oauthUser));
    }

    private Mono<OAuth2User> upsertAndBuildPrincipal(OAuth2UserRequest userRequest, OAuth2User oauthUser) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google / facebook
        Map<String, Object> attrs = oauthUser.getAttributes();

        String email = extractEmail(registrationId, attrs);
        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalStateException("OAuth2 provider did not return email"));
        }

        String displayName = extractName(registrationId, attrs);

        // ✅ 你可在這裡：首次登入建立 user、更新 last_login、同步 name、provider 等
        return userAuthService.upsertUserByEmail(email, displayName, registrationId)
                .flatMap(appUser -> userAuthService.loadRoles(appUser.getId())
                        .doOnNext(roles -> log.info("DB roles for {}: {}", email,
                                roles.stream().map(AppRole::getCode).toList()))
                        .map(roles -> toPrincipal(attrs, roles))
                );
    }

    private OAuth2User toPrincipal(Map<String, Object> attrs, List<AppRole> roles) {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode())) // e.g. ADMIN -> ROLE_ADMIN
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("Authorities = {}", authorities);

        // nameAttributeKey：通常用 "sub"(google) 或 "id"(facebook)；保守做法：挑一個存在的 key
        String nameKey = attrs.containsKey("sub") ? "sub" : (attrs.containsKey("id") ? "id" : "email");

        return new DefaultOAuth2User(authorities, attrs, nameKey);
    }

    private String extractEmail(String registrationId, Map<String, Object> attrs) {
        // Google / Facebook 通常都會有 email（但 Facebook 取決於權限 & 使用者是否有 email）
        Object email = attrs.get("email");
        return email == null ? null : String.valueOf(email);
    }

    private String extractName(String registrationId, Map<String, Object> attrs) {
        Object name = attrs.get("name");
        if (name != null) return String.valueOf(name);

        // 有些 provider 用不同欄位
        Object given = attrs.get("given_name");
        Object family = attrs.get("family_name");
        if (given != null || family != null) {
            return (given == null ? "" : given) + " " + (family == null ? "" : family);
        }
        return null;
    }
}