package com.merdeleine.gatewaybff.auth.dto;

public record UserLookupResponse(
        String customerId,
        String email,
        String displayName,
        String provider,
        String contactName,
        String contactPhone,
        String contactEmail,
        String shippingAddress,
        boolean profileCompleted
) {}

