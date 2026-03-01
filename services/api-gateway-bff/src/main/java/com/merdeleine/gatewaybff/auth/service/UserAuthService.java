package com.merdeleine.gatewaybff.auth.service;

import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.entity.AppUser;
import com.merdeleine.gatewaybff.auth.repo.AppUserRepository;
import com.merdeleine.gatewaybff.auth.repo.AppUserRoleRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserAuthService {

    private final AppUserRepository appUserRepository;
    private final AppUserRoleRepository appUserRoleRepository;

    public UserAuthService(AppUserRepository appUserRepository,
                           AppUserRoleRepository appUserRoleRepository) {
        this.appUserRepository = appUserRepository;
        this.appUserRoleRepository = appUserRoleRepository;
    }

    public Mono<AppUser> upsertUserByEmail(String email, String displayName, String provider) {
        return Mono.fromCallable(() -> {
                    Optional<AppUser> existing = appUserRepository.findByEmail(email);
                    AppUser u = existing.orElseGet(() -> {
                        AppUser nu = new AppUser();
                        nu.setId(UUID.randomUUID());
                        nu.setEmail(email);
                        nu.setCreatedAt(OffsetDateTime.now());
                        return nu;
                    });

                    u.setDisplayName(displayName);
                    u.setProvider(provider);
                    u.setLastLoginAt(OffsetDateTime.now());
                    return appUserRepository.save(u);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<AppRole>> loadRoles(UUID userId) {
        return Mono.fromCallable(() -> appUserRoleRepository.findRolesByUserId(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}