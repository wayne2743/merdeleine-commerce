package com.merdeleine.gatewaybff.auth.repo;


import com.merdeleine.gatewaybff.auth.entity.AppRole;
import com.merdeleine.gatewaybff.auth.entity.AppUserRole;
import com.merdeleine.gatewaybff.auth.entity.AppUserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AppUserRoleRepository extends JpaRepository<AppUserRole, AppUserRoleId> {

    @Query("""
        select r
        from AppRole r
        join AppUserRole ur on ur.roleId = r.id
        where ur.userId = :userId
    """)
    List<AppRole> findRolesByUserId(@Param("userId") UUID userId);
}