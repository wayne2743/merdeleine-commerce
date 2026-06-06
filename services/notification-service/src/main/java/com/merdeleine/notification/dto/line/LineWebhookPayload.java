package com.merdeleine.notification.dto.line;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineWebhookPayload(String destination, List<LineEvent> events) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineEvent(
            String type,         // "follow", "unfollow", "message", "accountLink"
            String replyToken,
            LineSource source,
            LineMessage message,
            LineLink link
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineSource(String type, String userId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineMessage(String type, String id, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineLink(String result, String nonce) {}
}
