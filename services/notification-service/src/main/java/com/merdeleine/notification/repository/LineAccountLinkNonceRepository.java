package com.merdeleine.notification.repository;

import com.merdeleine.notification.entity.LineAccountLinkNonce;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LineAccountLinkNonceRepository extends JpaRepository<LineAccountLinkNonce, UUID> {
    Optional<LineAccountLinkNonce> findByNonce(String nonce);
}
