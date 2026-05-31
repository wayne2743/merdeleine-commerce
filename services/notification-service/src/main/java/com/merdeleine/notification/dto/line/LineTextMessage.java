package com.merdeleine.notification.dto.line;

public record LineTextMessage(String type, String text) {
    public static LineTextMessage of(String text) {
        return new LineTextMessage("text", text);
    }
}
