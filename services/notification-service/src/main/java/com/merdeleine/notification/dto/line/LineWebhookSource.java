package com.merdeleine.notification.dto.line;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineWebhookSource(String type, String userId) {
}
