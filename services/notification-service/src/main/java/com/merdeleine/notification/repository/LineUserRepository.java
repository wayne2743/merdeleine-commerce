package com.merdeleine.notification.repository;

import com.merdeleine.notification.entity.LineUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LineUserRepository extends JpaRepository<LineUser, UUID> {
    Optional<LineUser> findByUserId(String userId);
    Optional<LineUser> findByMemberId(UUID memberId);
    Optional<LineUser> findByMemberIdAndFollowedTrue(UUID memberId);
    boolean existsByUserId(String userId);
    boolean existsByMemberId(UUID memberId);
}
