package com.merdeleine.gatewaybff.auth.service;

import com.merdeleine.gatewaybff.auth.dto.CompleteRegistrationRequest;
import com.merdeleine.gatewaybff.auth.dto.UpdateUserProfileRequest;
import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.entity.AppUser;
import com.merdeleine.gatewaybff.auth.entity.AppUserRole;
import com.merdeleine.gatewaybff.auth.repo.AppRoleRepository;
import com.merdeleine.gatewaybff.auth.repo.AppUserRepository;
import com.merdeleine.gatewaybff.auth.repo.AppUserRoleRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserAuthService {

    private final AppUserRepository appUserRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final AppRoleRepository appRoleRepository;

    public UserAuthService(AppUserRepository appUserRepository,
                           AppUserRoleRepository appUserRoleRepository,
                           AppRoleRepository appRoleRepository) {
        this.appUserRepository = appUserRepository;
        this.appUserRoleRepository = appUserRoleRepository;
        this.appRoleRepository = appRoleRepository;
    }

    public Mono<AppUser> upsertUserByEmail(String email, String displayName, String provider) {
        return Mono.fromCallable(() -> {
                    Optional<AppUser> existing = appUserRepository.findByEmail(email);
                    boolean isNew = existing.isEmpty();
                    AppUser u = existing.orElseGet(() -> {
                        AppUser nu = new AppUser();
                        nu.setId(UUID.randomUUID());
                        nu.setEmail(email);
                        nu.setCreatedAt(OffsetDateTime.now());
                        nu.setProfileCompleted(false); // 新用戶尚未完成註冊
                        return nu;
                    });

                    u.setDisplayName(displayName);
                    u.setProvider(provider);
                    u.setLastLoginAt(OffsetDateTime.now());
                    return appUserRepository.save(u);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 新用戶完成註冊（填寫聯絡資訊），設定 profileCompleted = true
     * 並自動指派 USER 角色（若尚未擁有）
     */
    public Mono<AppUser> completeRegistration(UUID userId, CompleteRegistrationRequest req) {
        return Mono.fromCallable(() -> {
                    AppUser u = appUserRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    u.setContactName(req.contactName());
                    u.setContactPhone(req.contactPhone());
                    if (req.contactEmail() != null) u.setContactEmail(req.contactEmail());
                    if (req.shippingAddress() != null) u.setShippingAddress(req.shippingAddress());
                    u.setProfileCompleted(true);
                    appUserRepository.save(u);

                    // 若尚未擁有 USER 角色，則新增一筆 app_user_role
                    AppRole userRole = appRoleRepository.findByCode("USER")
                            .orElseThrow(() -> new IllegalStateException("Role USER not found in DB"));
                    boolean alreadyHasRole = appUserRoleRepository
                            .findRolesByUserId(userId)
                            .stream()
                            .anyMatch(r -> "USER".equals(r.getCode()));
                    if (!alreadyHasRole) {
                        appUserRoleRepository.save(new AppUserRole(userId, userRole.getId()));
                    }

                    return u;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AppUser> updateProfile(String email, UpdateUserProfileRequest req) {
        return Mono.fromCallable(() -> {
                    AppUser u = appUserRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
                    applyProfileUpdates(u, req);
                    return appUserRepository.save(u);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AppUser> updateProfileByUserId(UUID userId, UpdateUserProfileRequest req) {
        return Mono.fromCallable(() -> {
                    AppUser u = appUserRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    applyProfileUpdates(u, req);
                    return appUserRepository.save(u);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void applyProfileUpdates(AppUser user, UpdateUserProfileRequest req) {
        if (req.displayName() != null) user.setDisplayName(req.displayName());
        if (req.contactName() != null) user.setContactName(req.contactName());
        if (req.contactPhone() != null) user.setContactPhone(req.contactPhone());
        if (req.contactEmail() != null) user.setContactEmail(req.contactEmail());
        if (req.shippingAddress() != null) user.setShippingAddress(req.shippingAddress());
    }

    public Mono<List<AppRole>> loadRoles(UUID userId) {
        return Mono.fromCallable(() -> appUserRoleRepository.findRolesByUserId(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Optional<AppUser>> findByCustomerId(UUID customerId) {
        return Mono.fromCallable(() -> appUserRepository.findById(customerId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<AppUser>> findByCustomerIds(List<UUID> customerIds) {
        return Mono.fromCallable(() -> {
                    LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(customerIds);
                    List<AppUser> users = appUserRepository.findAllById(uniqueIds);

                    Map<UUID, AppUser> byId = users.stream()
                            .collect(Collectors.toMap(AppUser::getId, Function.identity()));

                    List<AppUser> ordered = new ArrayList<>();
                    for (UUID id : uniqueIds) {
                        AppUser user = byId.get(id);
                        if (user != null) {
                            ordered.add(user);
                        }
                    }
                    return ordered;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}