package com.merdeleine.catalog.entity;

import com.merdeleine.catalog.enums.SellWindowStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sell_window")
public class SellWindow {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, length = 50)
    private String timezone;

    // NEW
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellWindowStatus status = SellWindowStatus.DRAFT;

    // NEW
    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    // NEW
    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = SellWindowStatus.DRAFT;
    }

    // getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public SellWindowStatus getStatus() { return status; }
    public void setStatus(SellWindowStatus status) { this.status = status; }

    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }

    public long getVersion() { return version; }
}
