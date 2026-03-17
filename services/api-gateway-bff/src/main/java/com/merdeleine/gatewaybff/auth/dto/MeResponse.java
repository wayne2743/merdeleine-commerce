package com.merdeleine.gatewaybff.auth.dto;

import java.util.List;

public record MeResponse(
        String id,
        String email,
        String displayName,
        String provider,
        List<String> roles,
        String token,          // JWT，前端存起來用
        String contactName,
        String contactPhone,
        String contactEmail,
        String shippingAddress
) {}