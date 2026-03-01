package com.merdeleine.gatewaybff.auth.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class AppUserRoleId implements Serializable {
    private UUID userId;
    private UUID roleId;

    public AppUserRoleId() {}

    public AppUserRoleId(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppUserRoleId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}