package com.merdeleine.notification.dto.line;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineWebhookEvent(
        String type,
        LineWebhookSource source,
        LineWebhookLink link
) {
}
