package com.merdeleine.gatewaybff.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新使用者聯絡資訊 — PUT /auth/me
 */
public record UpdateUserProfileRequest(

        @Size(max = 255)
        String displayName,

        @Size(max = 100)
        String contactName,

        @Size(max = 30)
        String contactPhone,

        @Email
        @Size(max = 255)
        String contactEmail,

        @Size(max = 1000)
        String shippingAddress
) {}

