package com.merdeleine.notification.dto.line;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineWebhookLink(String result, String nonce) {
}
