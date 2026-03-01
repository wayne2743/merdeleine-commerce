package com.merdeleine.gatewaybff.auth.security;

import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.service.UserAuthService;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
public class DbBackedReactiveOidcUserService implements ReactiveOAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();
    private final UserAuthService userAuthService;

    public DbBackedReactiveOidcUserService(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) {
        return delegate.loadUser(userRequest)
                .flatMap(oidcUser -> {
                    String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google
                    String email = oidcUser.getEmail(); // OIDC 標準欄位（Google OK）
                    if (email == null || email.isBlank()) {
                        return Mono.error(new IllegalStateException("OIDC provider did not return email"));
                    }

                    String displayName = oidcUser.getFullName(); // 或 oidcUser.getAttribute("name")

                    return userAuthService.upsertUserByEmail(email, displayName, registrationId)
                            .flatMap(appUser -> userAuthService.loadRoles(appUser.getId())
                                    .map(roles -> toPrincipal(oidcUser, roles))
                            );
                });
    }

    private OidcUser toPrincipal(OidcUser oidcUser, List<AppRole> roles) {
        Set<GrantedAuthority> roleAuthorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 保留原本 OIDC 的 authorities（OIDC_USER / SCOPE_*）
        Set<GrantedAuthority> merged = new LinkedHashSet<>();
        merged.addAll(oidcUser.getAuthorities());
        merged.addAll(roleAuthorities);

        // nameAttributeKey 通常用 "sub"
        return new DefaultOidcUser(merged, oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
    }
}