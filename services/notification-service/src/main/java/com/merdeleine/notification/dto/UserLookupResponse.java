package com.merdeleine.notification.dto;

public record UserLookupResponse(
        String customerId,
        String email,
        String displayName,
        String contactName,
        String contactEmail
) {
}

