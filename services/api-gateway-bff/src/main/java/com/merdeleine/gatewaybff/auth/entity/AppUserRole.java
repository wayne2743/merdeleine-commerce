package com.merdeleine.gatewaybff.auth.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "app_user_role")
@IdClass(AppUserRoleId.class)
public class AppUserRole {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "role_id", columnDefinition = "uuid")
    private UUID roleId;

    public AppUserRole() {}

    public AppUserRole(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() { return userId; }
    public UUID getRoleId() { return roleId; }
}