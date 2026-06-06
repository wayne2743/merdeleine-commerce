package com.merdeleine.notification.dto.line;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineWebhookRequest(List<LineWebhookEvent> events) {
}
