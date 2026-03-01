package com.merdeleine.gatewaybff.auth.repo;


import com.merdeleine.gatewaybff.auth.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {
    Optional<AppRole> findByCode(String code);
}