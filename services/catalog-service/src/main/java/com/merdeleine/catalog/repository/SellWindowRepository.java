package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.SellWindowStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellWindowRepository extends JpaRepository<SellWindow, UUID> {
    Optional<SellWindow> findByName(String name);
    boolean existsByName(String name);

    // 只抓到期且仍 OPEN 的候選（不鎖）
    @Query("""
        select sw.id as id, sw.version as version
        from SellWindow sw
        where sw.status = :status
          and sw.endAt <= :now
        order by sw.endAt asc
    """)
    List<SellWindowCandidate> findExpiredOpenCandidates(
            @Param("now") OffsetDateTime now,
            @Param("status") SellWindowStatus status,
            Pageable pageable
    );

    // ✅ version CAS：只有符合 id + status OPEN + endAt<=now + version 才能關單成功
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SellWindow sw
        set sw.status = com.merdeleine.catalog.enums.SellWindowStatus.CLOSED,
            sw.closedAt = :now,
            sw.version = sw.version + 1
        where sw.id = :id
          and sw.status = com.merdeleine.catalog.enums.SellWindowStatus.OPEN
          and sw.endAt <= :now
          and sw.version = :version
    """)
    int closeIfExpiredOpenAndVersionMatch(
            @Param("id") UUID id,
            @Param("version") long version,
            @Param("now") OffsetDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sw from SellWindow sw where sw.id = :id")
    Optional<SellWindow> findByIdForUpdate(@Param("id") UUID id);

}


