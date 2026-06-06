package com.merdeleine.notification.entity;

import com.merdeleine.notification.enums.LineAccountLinkStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "line_account_link_nonce")
public class LineAccountLinkNonce {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String nonce;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "line_user_id", nullable = false, length = 100)
    private String lineUserId;

    @Column(name = "link_token", nullable = false, length = 255)
    private String linkToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LineAccountLinkStatus status;

    @Column(name = "expired_at", nullable = false)
    private OffsetDateTime expiredAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = LineAccountLinkStatus.PENDING;
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }

    public String getLineUserId() { return lineUserId; }
    public void setLineUserId(String lineUserId) { this.lineUserId = lineUserId; }

    public String getLinkToken() { return linkToken; }
    public void setLinkToken(String linkToken) { this.linkToken = linkToken; }

    public LineAccountLinkStatus getStatus() { return status; }
    public void setStatus(LineAccountLinkStatus status) { this.status = status; }

    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }

    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
